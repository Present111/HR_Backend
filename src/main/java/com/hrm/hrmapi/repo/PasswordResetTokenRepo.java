package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PasswordResetTokenRepo extends MongoRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    // Tuỳ chọn: lấy token active mới nhất của user
    Optional<PasswordResetToken> findTopByUserIdAndUsedFalseOrderByExpiresAtDesc(String userId);

    // Tuỳ chọn: xoá toàn bộ token của user (nếu muốn cleanup)
    void deleteByUserId(String userId);
}
