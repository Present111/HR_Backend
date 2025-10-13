// src/main/java/com/hrm/hrmapi/security/PasswordResetService.java
package com.hrm.hrmapi.security;

import com.hrm.hrmapi.domain.PasswordResetToken;
import com.hrm.hrmapi.repo.PasswordResetTokenRepo;
import com.hrm.hrmapi.repo.UserRepo;
import com.hrm.hrmapi.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepo users;
    private final PasswordResetTokenRepo tokens;

    public String issueToken(String email) {
        var u = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại"));

        // (tuỳ chọn) vô hiệu hoá token cũ còn active của user này
        tokens.findByTokenAndUsedFalse(u.getId()); // chỉ để IDE import, không dùng ở đây

        var token = UUID.randomUUID().toString().replace("-", "");
        var ent = PasswordResetToken.builder()
                .userId(u.getId())
                .token(token)
                .expiresAt(Instant.now().plusSeconds(30 * 60)) // 30 phút
                .used(false)
                .build();
        tokens.save(ent);
        return token; // dev: trả ra để FE test nhanh
    }

    public User consumeToken(String token) {
        var prt = tokens.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token không hợp lệ hoặc đã dùng"));

        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token đã hết hạn");
        }

        prt.setUsed(true);
        tokens.save(prt);

        return users.findById(prt.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user"));
    }
}
