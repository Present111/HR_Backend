package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.Employee;
import com.hrm.hrmapi.domain.User;
import com.hrm.hrmapi.repo.EmployeeRepo;
import com.hrm.hrmapi.repo.UserRepo;
import com.hrm.hrmapi.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Users")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserRepo users;
    private final EmployeeRepo employees;
    private final PasswordEncoder pe;
    private final FileStorageService fileStorage;

    /* ===================== DTOs ===================== */

    // JSON body (có thể kèm fullName / avatarUrl)
    public record CreateUserRequest(
            @Schema(example = "emp.new@hrm.local") @NotBlank @Email String email,
            @Schema(example = "EMPLOYEE", description = "ADMIN | MANAGER | EMPLOYEE")
            @NotBlank String role,
            @Schema(example = "123456") @NotBlank String tempPassword,
            @Schema(example = "emp_01",
                    description = "Bắt buộc nếu role=EMPLOYEE. Bỏ trống nếu ADMIN/MANAGER")
            String employeeId,
            @Schema(example = "Nguyễn Văn A",
                    description = "Tuỳ chọn, dùng cho ADMIN/MANAGER. EMPLOYEE sẽ lấy từ Employee")
            String fullName,
            @Schema(example = "http://localhost:8080/uploads/abc.png",
                    description = "Tuỳ chọn: nếu đã có URL ảnh. Nếu upload file thì không cần")
            String avatarUrl
    ) {}

    public record UpdateUserRequest(
            @Schema(example = "MANAGER", description = "ADMIN | MANAGER | EMPLOYEE") String role,
            @Schema(example = "emp_01", description = "Bắt buộc nếu role=EMPLOYEE") String employeeId
    ) {}

    public record AdminSetPasswordRequest(
            @Schema(example = "NewStrongPass!234") @NotBlank String newPassword
    ) {}

    /* ===================== Helpers ===================== */

    private String absoluteUrl(String maybePublicPath) {
        if (maybePublicPath == null || maybePublicPath.isBlank()) return null;
        String lower = maybePublicPath.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) return maybePublicPath;
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(maybePublicPath.startsWith("/") ? maybePublicPath : "/" + maybePublicPath)
                .toUriString();
    }

    private Map<String, Object> toUserMap(User u) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("fullName", u.getFullName());
        m.put("role", u.getRole() != null ? u.getRole().name() : null);
        m.put("employeeId", u.getEmployeeId());
        m.put("avatarUrl", u.getAvatarUrl());
        return m;
    }

    private String titleCaseFromEmail(String email) {
        String local = email.split("@")[0];
        local = local.replace('.', ' ').replace('_',' ').trim();
        if (local.isEmpty()) return null;
        String[] parts = local.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    private Object createCore(String email, String roleStr, String tempPassword,
                              String employeeIdInput, String fullNameInput,
                              MultipartFile avatarFile, String avatarUrlFromJson) {

        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        final User.Role role;
        try {
            role = User.Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }

        String employeeId = null;
        String fullName = null;

        if (role == User.Role.EMPLOYEE) {
            if (employeeIdInput == null || employeeIdInput.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "employeeId is required for EMPLOYEE");
            }
            var emp = employees.findById(employeeIdInput)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
            if (users.existsByEmployeeId(emp.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This employee already has a user");
            }
            employeeId = emp.getId();
            // <-- Tự lấy fullName từ Employee
            fullName = emp.getFullName();
        } else {
            // ADMIN/MANAGER: dùng fullName truyền vào; nếu không có thì đoán theo email
            if (fullNameInput != null && !fullNameInput.isBlank()) fullName = fullNameInput;
            else fullName = titleCaseFromEmail(email);
        }

        // Avatar: ưu tiên file upload; nếu không có file thì dùng avatarUrl (nếu có)
        String publicPath = null;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            publicPath = fileStorage.saveAvatar(avatarFile); // trả "/uploads/xxx.png"
        } else if (avatarUrlFromJson != null && !avatarUrlFromJson.isBlank()) {
            publicPath = avatarUrlFromJson;
        }
        String avatarAbsolute = absoluteUrl(publicPath);

        var u = User.builder()
                .email(email)
                .fullName(fullName) // <-- KHÔNG còn null
                .passwordHash(pe.encode(tempPassword))
                .role(role)
                .employeeId(employeeId)
                .avatarUrl(avatarAbsolute)
                .build();

        var saved = users.save(u);
        return toUserMap(saved);
    }

    /* ===================== ENDPOINTS ===================== */

    @Operation(summary = "Danh sách người dùng")
    @GetMapping
    public Object all() {
        return users.findAll().stream().map(this::toUserMap).toList();
    }

    @Operation(
            summary = "Tạo tài khoản (JSON) – ADMIN/MANAGER. " +
                    "Nếu role=EMPLOYEE phải có employeeId. " +
                    "fullName: tuỳ chọn cho ADMIN/MANAGER (EMPLOYEE sẽ lấy từ Employee).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = CreateUserRequest.class),
                            examples = {
                                    @ExampleObject(name = "EMPLOYEE (auto lấy tên từ Employee)",
                                            value = """
                                            {
                                              "email": "emp.new@hrm.local",
                                              "role": "EMPLOYEE",
                                              "tempPassword": "123456",
                                              "employeeId": "emp_01"
                                            }
                                            """),
                                    @ExampleObject(name = "MANAGER (tự nhập tên + có avatarUrl)",
                                            value = """
                                            {
                                              "email": "manager2@hrm.local",
                                              "role": "MANAGER",
                                              "tempPassword": "123456",
                                              "fullName": "Trần Văn B",
                                              "avatarUrl": "http://localhost:8080/uploads/demo.png"
                                            }
                                            """)
                            })
            )
    )
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Object createJson(@Valid @RequestBody CreateUserRequest body) {
        return createCore(
                body.email(),
                body.role(),
                body.tempPassword(),
                body.employeeId(),
                body.fullName(),
                null,
                body.avatarUrl()
        );
    }

    @Operation(summary = "Tạo tài khoản (multipart) – KHÔNG cần avatarUrl. form-data: email, role, tempPassword, (employeeId), (fullName), (avatar)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Object createMultipart(
            @RequestPart("email") String email,
            @RequestPart("role") String role,
            @RequestPart("tempPassword") String tempPassword,
            @RequestPart(value = "employeeId", required = false) String employeeId,
            @RequestPart(value = "fullName", required = false) String fullName,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) {
        return createCore(email, role, tempPassword, employeeId, fullName, avatar, null);
    }

    @Operation(summary = "Chi tiết người dùng theo id")
    @GetMapping("/{id}")
    public Object getOne(@PathVariable String id) {
        var u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toUserMap(u);
    }

    @Operation(
            summary = "Cập nhật vai trò & liên kết employee (ADMIN/MANAGER). " +
                    "Nếu đổi sang EMPLOYEE thì bắt buộc employeeId và sẽ đồng bộ fullName từ Employee."
    )
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public Object update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest body) {
        var u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (body.role() != null) {
            final User.Role newRole;
            try {
                newRole = User.Role.valueOf(body.role().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
            }

            if (newRole == User.Role.EMPLOYEE) {
                if (body.employeeId() == null || body.employeeId().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "employeeId is required for EMPLOYEE");
                }
                Employee emp = employees.findById(body.employeeId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
                if (users.existsByEmployeeId(emp.getId()) && !emp.getId().equals(u.getEmployeeId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This employee already has a user");
                }
                u.setEmployeeId(emp.getId());
                // đồng bộ tên
                u.setFullName(emp.getFullName());
            } else {
                // chuyển khỏi EMPLOYEE: bỏ liên kết nếu caller truyền employeeId (tuỳ business)
                if (body.employeeId() != null) {
                    u.setEmployeeId(null);
                }
                // giữ nguyên fullName hiện tại (hoặc bạn có thể fallback theo email nếu đang null)
                if (u.getFullName() == null || u.getFullName().isBlank()) {
                    u.setFullName(titleCaseFromEmail(u.getEmail()));
                }
            }
            u.setRole(newRole);
        }

        users.save(u);
        return Map.of("message", "User updated");
    }

    @Operation(summary = "ADMIN/MANAGER đặt lại mật khẩu cho user")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}/password")
    public Object adminSetPassword(@PathVariable String id,
                                   @RequestBody @Valid AdminSetPasswordRequest req) {
        var u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        u.setPasswordHash(pe.encode(req.newPassword()));
        users.save(u);
        return Map.of("message", "Password updated");
    }

    @Operation(summary = "Cập nhật avatar cho user (multipart) – part 'avatar'")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping(path = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object updateAvatar(@PathVariable String id,
                               @RequestPart("avatar") MultipartFile avatar) {
        var u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (avatar == null || avatar.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file is required");
        }

        String publicPath = fileStorage.saveAvatar(avatar);   // "/uploads/xxx.png"
        String absolute = absoluteUrl(publicPath);

        u.setAvatarUrl(absolute);
        users.save(u);

        return Map.of("message", "Avatar updated", "avatarUrl", absolute);
    }
}
