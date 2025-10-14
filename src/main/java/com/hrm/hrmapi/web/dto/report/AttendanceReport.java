package com.hrm.hrmapi.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceReport {
    private String month;                    // YYYY-MM
    private String department;               // optional
    private List<AttendanceRow> rows;
    private AttendanceMonthlySummary summary;
}
