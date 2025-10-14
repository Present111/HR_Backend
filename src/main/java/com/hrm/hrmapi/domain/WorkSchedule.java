package com.hrm.hrmapi.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Instant;
import java.util.Set;

@Document("work_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkSchedule {
    @Id
    private String id;

    private String name;                        // e.g. "Default"

    private LocalTime startTime;                // 09:00
    private LocalTime endTime;                  // 18:00

    private Integer breakMinutes;               // 60 (nếu cần)

    private Integer graceLateMinutes;           // phút cho phép trễ (ví dụ 5)
    private Integer graceEarlyMinutes;          // phút cho phép về sớm (ví dụ 0)

    private Integer otAfterMinutes;             // chỉ tính OT nếu > X phút sau giờ end (ví dụ 30)
    private Integer otRoundToMinutes;           // làm tròn OT theo bội số (ví dụ 15)

    private Set<DayOfWeek> workingDays;         // mặc định MON..FRI

    private Instant updatedAt;
}