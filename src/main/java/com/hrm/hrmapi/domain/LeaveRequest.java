package com.hrm.hrmapi.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document("leave_requests")
@Data @NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {
    @Id private String id;

    @Indexed private String employeeId;
    private String typeCode;

    private LocalDate startDate;
    private String startSession;
    private LocalDate endDate;
    private String endSession;
    private double days;

    private String status;
    private String approverId;
    private String reason;
    private String managerNote;

    private String createdBy;
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;
}
