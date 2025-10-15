package com.hrm.hrmapi.payroll;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document("payslips")
public class Payslip {
    @Id private String id;

    private String cycleId;
    private String employeeId;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Item {
        private String componentId;
        private String label;
        private String kind;             // "EARNING" | "DEDUCTION"
        private BigDecimal amount;
    }

    private String currency;
    private LocalDate calcAt;
    private AttendanceSummary summary;

    private List<Item> items;
    private BigDecimal gross;
    private BigDecimal deductions;
    private BigDecimal net;

    private String status;             // CALCULATED | APPROVED | PAID
    private Instant generatedAt;
}
