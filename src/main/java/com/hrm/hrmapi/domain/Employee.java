// domain/Employee.java
package com.hrm.hrmapi.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Document("employees")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Employee {
    @Id private String id;

    @Indexed(unique = true)          // mã nhân viên duy nhất
    private String code;

    @Indexed                         // hay lọc theo phòng ban
    private String department;

    private String fullName;
    private String position;
    private String status;      // ACTIVE / INACTIVE
    private LocalDate joinDate;
    private String phone;
    private String address;

    private Emergency emergencyContact;

    @Data public static class Emergency {
        private String name; private String phone; private String relation;
    }
}
