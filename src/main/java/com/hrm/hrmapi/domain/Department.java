// domain/Department.java
package com.hrm.hrmapi.domain;
import lombok.*; import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("departments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Department {
    @Id private String id;
    private String name;
    private String manager;
}
