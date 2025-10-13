package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.Employee;
import com.hrm.hrmapi.domain.PasswordResetToken;
import com.hrm.hrmapi.domain.User;
import com.hrm.hrmapi.repo.EmployeeRepo;
import com.hrm.hrmapi.repo.PasswordResetTokenRepo;
import com.hrm.hrmapi.repo.UserRepo;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Users")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserRepo users;
    private final EmployeeRepo employees;
    private final PasswordEncoder pe;
    private final PasswordResetTokenRepo resetTokens;

    /* ===================== DTOs ===================== */

    public record CreateUserRequest(
            @Schema(example = "emp.new@hrm.local") @NotBlank @Email String email,
            @Schema(example = "EMPLOYEE", description = "ADMIN | MANAGER | EMPLOYEE")
            @NotBlank String role,
            @Schema(example = "123456") @NotBlank String tempPassword,
            @Schema(example = "emp_01",
                    description = "Bắt buộc nếu role = EMPLOYEE. Để trống nếu tạo MANAGER/ADMIN")
            String employeeId
    ) {}

    public record UpdateUserRequest(
            @Schema(example = "MANAGER", description = "ADMIN | MANAGER | EMPLOYEE") String role,
            @Schema(example = "emp_01", description = "Bắt buộc nếu role=EMPLOYEE") String employeeId
    ) {}

    public record AdminSetPasswordRequest(
            @Schema(example = "NewStrongPass!234") @NotBlank String newPassword
    ) {}

    /* ===================== ENDPOINTS ===================== */

    @Operation(summary = "Danh sách người dùng")
    @GetMapping
    public Object all() {
        return users.findAll().stream().map(u -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("id", u.getId());
            m.put("email", u.getEmail());
            m.put("fullName", u.getFullName());              // có thể null
            m.put("role", u.getRole() != null ? u.getRole().name() : null);
            m.put("employeeId", u.getEmployeeId());          // có thể null
            return m;
        }).toList();
    }

    @Operation(
            summary = "Tạo tài khoản (chỉ ADMIN/MANAGER). " +
                    "Nếu role = EMPLOYEE phải truyền employeeId để liên kết hồ sơ nhân viên",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = CreateUserRequest.class),
                            examples = {
                                    @ExampleObject(name = "Tạo tài khoản cho nhân viên",
                                            value = """
                        {
                          "email": "emp.new@hrm.local",
                          "role": "EMPLOYEE",
                          "tempPassword": "123456",
                          "employeeId": "emp_01"
                        }
                      """),
                                    @ExampleObject(name = "Tạo manager không gắn employee",
                                            value = """
                        {
                          "email": "manager2@hrm.local",
                          "role": "MANAGER",
                          "tempPassword": "123456"
                        }
                      """)
                            })
            )
    )
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Object create(@Valid @RequestBody CreateUserRequest body) {
        // email trùng?
        if (users.existsByEmail(body.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        // role hợp lệ?
        final User.Role role;
        try {
            role = User.Role.valueOf(body.role().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }

        // Nếu EMPLOYEE → cần employeeId còn trống user
        String employeeId = null;
        if (role == User.Role.EMPLOYEE) {
            if (body.employeeId() == null || body.employeeId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "employeeId is required for EMPLOYEE");
            }
            Employee emp = employees.findById(body.employeeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
            if (users.existsByEmployeeId(emp.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This employee already has a user");
            }
            employeeId = emp.getId();
        }

        var u = User.builder()
                .email(body.email())
                .fullName(null) // có thể copy từ Employee nếu muốn
                .passwordHash(pe.encode(body.tempPassword()))
                .role(role)
                .employeeId(employeeId)
                .build();

        var saved = users.save(u);
        return Map.of(
                "id", saved.getId(),
                "email", saved.getEmail(),
                "role", saved.getRole().name(),
                "employeeId", saved.getEmployeeId()
        );
    }

    @Operation(summary = "Chi tiết người dùng theo id")
    @GetMapping("/{id}")
    public Object getOne(@PathVariable String id) {
        var u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        var m = new LinkedHashMap<String, Object>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("fullName", u.getFullName());
        m.put("role", u.getRole() != null ? u.getRole().name() : null);
        m.put("employeeId", u.getEmployeeId());
        return m;
    }

    @Operation(
            summary = "Cập nhật vai trò và liên kết employee (ADMIN/MANAGER). " +
                    "Nếu đổi sang EMPLOYEE phải cung cấp employeeId hợp lệ chưa bị chiếm"
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

            // Nếu sang EMPLOYEE → cần employeeId hợp lệ + chưa có user khác
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
            } else {
                // Không phải EMPLOYEE → bỏ liên kết nếu muốn
                if (body.employeeId() != null) {
                    u.setEmployeeId(null);
                }
            }
            u.setRole(newRole);
        }

        users.save(u);
        return Map.of("message", "User updated");
    }

}
