package com.hrm.hrmapi.payroll;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import static com.hrm.hrmapi.payroll.PayrollEnums.CalcType;
import static com.hrm.hrmapi.payroll.PayrollEnums.ComponentKind;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document("payroll_components")
public class PayrollComponent {
    @Id
    private String id;                 // "BASE_SALARY", "OT_WEEKDAY"...
    @Indexed(unique = true)
    private String label;              // tên hiển thị
    private ComponentKind kind;        // EARNING | DEDUCTION
    private CalcType calcType;         // FIXED | FORMULA
    private String expr;               // dùng khi calcType = FORMULA
    private Integer priority;          // thứ tự tính
    private boolean active;
}
