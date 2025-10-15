// domain/Contract.java
package com.hrm.hrmapi.domain;
import lombok.*; import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.math.BigDecimal; import java.time.LocalDate;

@Document("contracts")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Contract {
    @MongoId(value = FieldType.OBJECT_ID)   // <-- ép lưu ObjectId nhưng field vẫn là String
    private String id;
    private String employeeId;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal baseSalary;
    private String status;     // ACTIVE EXPIRED
    private int version;
}
