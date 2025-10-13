package com.hrm.hrmapi.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("password_reset_tokens")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PasswordResetToken {
    @Id private String id;

    @Indexed
    private String userId;

    @Indexed(unique = true)
    private String token;

    // TTL: Mongo sẽ tự xoá doc sau khi quá hạn
    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;

    private boolean used;
}
