package com.hrm.hrmapi.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("leave_types")
@Data @Builder @NoArgsConstructor
@AllArgsConstructor
public class LeaveType {
    @Id private String id;

    private String code;
    private String name;
    private boolean deductQuota;

}
