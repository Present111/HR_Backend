package com.hrm.hrmapi.web.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceRow {
    private String employeeId;
    private String employeeName;
    private String department;
    private String date;       // YYYY-MM-DD
    private String status;     // PRESENT WFH LEAVE HOLIDAY ABSENT
    private String checkIn;    // HH:mm:ss
    private String checkOut;   // HH:mm:ss
    private Integer lateMinutes;
    private Integer earlyMinutes;
    private Integer otMinutes;
}
