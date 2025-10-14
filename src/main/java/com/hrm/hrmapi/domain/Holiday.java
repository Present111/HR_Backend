package com.hrm.hrmapi.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document("holidays")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Holiday {
    @Id private String id;

    @Indexed private LocalDate date;
    private String name;
    private String region;   // optional (VN, HCM, ...)
}
