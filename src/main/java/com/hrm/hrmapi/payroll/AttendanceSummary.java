package com.hrm.hrmapi.payroll;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceSummary {
    private int workingDaysPaid;         // số ngày công tính lương
    private int workingDaysInCycle;      // tổng ngày làm việc chuẩn trong kỳ (trừ T7, CN, lễ nếu policy như vậy)
    private int unpaidLeaveDays;         // ngày nghỉ không lương
    private int lateMinutes;             // đi muộn
    private int earlyLeaveMinutes;       // về sớm

    private int otMinutesWeekday;        // OT ngày thường
    private int otMinutesWeekend;        // OT cuối tuần
    private int otMinutesHoliday;        // OT ngày lễ

    private BigDecimal baseSalary;       // từ Contract đang hiệu lực
    private BigDecimal baseHourly;       // baseSalary / (workingDaysInCycle * 8)
}
