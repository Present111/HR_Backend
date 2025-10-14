package com.hrm.hrmapi.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceMonthlySummary {
    private int totalLateMinutes;
    private int totalEarlyMinutes;
    private int totalOtMinutes;
    private int totalPresentDays;
    private int totalLeaveDays;
    private int totalWfhDays;
    private int totalHolidayDays;
    private int totalAbsentDays;
}
