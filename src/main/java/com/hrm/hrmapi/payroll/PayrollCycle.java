package com.hrm.hrmapi.payroll;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

import static com.hrm.hrmapi.payroll.PayrollEnums.CycleStatus;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document("payroll_cycles")
public class PayrollCycle {
    @Id
    private String id;                 // ví dụ "2025-11"
    @Indexed(unique = true)
    private String name;               // "Nov 2025"
    private LocalDate startDate;
    private LocalDate endDate;
    private String currency;           // "VND"
    private CycleStatus status;        // DRAFT | LOCKED | PAID
    private String notes;
}
