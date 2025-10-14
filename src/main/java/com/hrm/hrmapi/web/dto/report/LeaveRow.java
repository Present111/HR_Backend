package com.hrm.hrmapi.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRow {
    private String requestId;
    private String employeeId;
    private String employeeName;
    private String department;
    private String type;      // ANNUAL SICK UNPAID...
    private String fromDate;  // YYYY-MM-DD
    private String toDate;    // YYYY-MM-DD
    private double days;      // đã trừ T7 CN và ngày lễ
    private String status;    // APPROVED REJECTED PENDING
}
