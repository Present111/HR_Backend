package com.hrm.hrmapi.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Document("attendance_records")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@CompoundIndex(name= "emp_date", def = "{'employeeId': 1, 'date': 1}",unique = true)
public class AttendanceRecord {
    @Id private String id;

    @Indexed private String employeeId;
    private LocalDate date;

    private LocalTime checkIn;
    private LocalTime checkOut;

    private String source;
    private String status;
    private Integer lateMinutes;
    private Integer earlyMinutes;
    private Integer otMinutes;

    private String note;
    private String batchId;
    private Instant createdAt;
    private Instant updatedAt;
}
