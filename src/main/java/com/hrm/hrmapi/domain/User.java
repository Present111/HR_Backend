// domain/User.java
package com.hrm.hrmapi.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Document("users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id private String id;

    @Indexed(unique = true)          // mỗi email là duy nhất
    private String email;

    private String fullName;

    private String avatarUrl;

    @JsonIgnore
    private String passwordHash;

    private Role role;               // ADMIN / MANAGER / EMPLOYEE

    @Indexed(unique = true, sparse = true)
    private String employeeId;       // 1 user ↔ 1 employee (nullable cho manager/đối tác)

    public enum Role { ADMIN, MANAGER, EMPLOYEE }
}
