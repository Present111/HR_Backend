package com.hrm.hrmapi.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

import java.util.List;

@Document("attendance_batches")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceBatch {
    @Id private String id;
    private String month;
    private String filename;
    private String importedBy;
    private Instant importedAt;

    private Integer totalRows;
    private Integer success;
    private Integer failed;
    private List<String> errors;
}
