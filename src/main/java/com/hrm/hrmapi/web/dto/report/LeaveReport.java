package com.hrm.hrmapi.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveReport {
    private String month;
    private String department;
    private List<LeaveRow> rows;
    private double totalDaysApproved;
    private double totalDaysPending;
}
