package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.PasswordResetToken;
import com.hrm.hrmapi.domain.User;
import com.hrm.hrmapi.repo.PasswordResetTokenRepo;
import com.hrm.hrmapi.repo.UserRepo;
import com.hrm.hrmapi.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Auth")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepo users;
    private final PasswordEncoder pe;
    private final JwtService jwt;
    private final PasswordResetTokenRepo resetTokens;

    /* ===================== DTOs ===================== */

    public record LoginRequest(
            @Schema(example = "admin@hrm.local") @NotBlank @Email String email,
            @Schema(example = "admin") @NotBlank String password
    ) {}

    public record MeResponse(
            String id, String email, String fullName, String role, String employeeId
    ) {}

    public record ForgotRequest(
            @Schema(example = "emp.new@hrm.local") @NotBlank @Email String email
    ) {}

    public record ResetRequest(
            @Schema(example = "xxxxx-xxxx-...") @NotBlank String token,
            @Schema(example = "NewStrongPass!234") @NotBlank String newPassword
    ) {}

    public record ChangePwdRequest(
            @Schema(example = "oldPass123") @NotBlank String oldPassword,
            @Schema(example = "NewStrongPass!234") @NotBlank String newPassword
    ) {}

    /* ===================== Endpoints ===================== */

    @Operation(
            summary = "Đăng nhập",
            description = "Nhận JWT và thông tin user để gọi các API bảo vệ",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(value = """
            {"email":"admin@hrm.local","password":"admin"}
          """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(example = """
                            {
                              "token": "<JWT>",
                              "user": {
                                "id": "68e86febd5e93b0593ad4129",
                                "email": "admin@hrm.local",
                                "fullName": "Admin",
                                "role": "ADMIN",
                                "employeeId": null
                              }
                            }
                            """))),
                    @ApiResponse(responseCode = "401", description = "Sai tài khoản/mật khẩu")
            }
    )
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest body) {
        // chuẩn hóa email để tránh trùng lặp hoa/thường
        String email = body.email().trim().toLowerCase();

        var u = users.findByEmail(email)
                .filter(x -> pe.matches(body.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        var userInfo = new MeResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRole() != null ? u.getRole().name() : null,
                u.getEmployeeId()
        );

        return Map.of(
                "ok", true,
                "token", jwt.issue(u.getId(), u.getRole().name()),
                "user", userInfo
        );
    }

    @Operation(summary = "Lấy thông tin người dùng hiện tại")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = MeResponse.class)))
    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        var u = (User) auth.getPrincipal();
        return new MeResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRole() != null ? u.getRole().name() : null,
                u.getEmployeeId()
        );
    }

    @Operation(summary = "Quên mật khẩu: nhập email, hệ thống tạo reset token (mock gửi mail)")
    @PostMapping("/forgot")
    public Map<String, Object> forgot(@Valid @RequestBody ForgotRequest req) {
        // luôn trả 200 để không lộ email có tồn tại hay không
        var userOpt = users.findByEmail(req.email().trim().toLowerCase());
        if (userOpt.isPresent()) {
            var token = UUID.randomUUID().toString();
            resetTokens.save(PasswordResetToken.builder()
                    .userId(userOpt.get().getId())
                    .token(token)
                    .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES)) // 30 phút
                    .used(false)
                    .build());
            // TODO: gửi email thực tế: https://fe/reset-password?token=<token>
            return Map.of(
                    "ok", true,
                    "message", "If the email exists, a reset link has been sent.",
                    // dev only: để dễ test
                    "mockResetToken", token
            );
        }
        return Map.of(
                "ok", true,
                "message", "If the email exists, a reset link has been sent."
        );
    }

    @Operation(summary = "Đặt lại mật khẩu bằng reset token")
    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@Valid @RequestBody ResetRequest req) {
        var prt = resetTokens.findByTokenAndUsedFalse(req.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        var user = users.findById(prt.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPasswordHash(pe.encode(req.newPassword()));
        users.save(user);

        prt.setUsed(true);
        resetTokens.save(prt);

        return Map.of("ok", true, "message", "Password has been reset successfully");
    }

    @Operation(summary = "Đổi mật khẩu (yêu cầu đăng nhập)")
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(Authentication auth,
                                              @Valid @RequestBody ChangePwdRequest body) {
        var u = (User) auth.getPrincipal();
        if (!pe.matches(body.oldPassword(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password incorrect");
        }
        u.setPasswordHash(pe.encode(body.newPassword()));
        users.save(u);
        return Map.of("ok", true, "message", "Password changed");
    }
}
