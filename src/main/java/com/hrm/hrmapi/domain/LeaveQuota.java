package com.hrm.hrmapi.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("leave_quotas")
@Data @Builder @NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "emp_year", def = "{'employeeId': 1, 'year': 1}", unique = true)
public class LeaveQuota {
    @Id private String id;

    private String employeeId;
    private Integer year;

    private double entitlement; // tiêu chuẩn năm (VD 12)
    private double carriedOver; // chuyển từ năm trước
    private double taken;       // đã duyệt
    private double remaining;   // entitlement + carriedOver - taken

    private Instant lastCalculatedAt;
}
