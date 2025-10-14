package com.hrm.hrmapi.service;

import com.hrm.hrmapi.domain.*;
import com.hrm.hrmapi.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepo attendanceRepo;
    private final AttendanceBatchRepo batchRepo;
    private final EmployeeRepo employeeRepo;
    private final HolidayRepo holidayRepo;
    private final WorkScheduleService scheduleService;

    private static final Set<DayOfWeek> WEEKENDS = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    /**
     * Import CSV attendance data
     */
    public AttendanceBatch importCsv(MultipartFile file, YearMonth ym, String importedBy) {
        var batch = AttendanceBatch.builder()
                .month(ym.toString())
                .filename(file.getOriginalFilename())
                .importedBy(importedBy)
                .importedAt(Instant.now())
                .totalRows(0).success(0).failed(0)
                .errors(new ArrayList<>())
                .build();
        batch = batchRepo.save(batch);

        try (var br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            // CSV header: employeeCode,fullName,date,checkIn,checkOut,source
            String line;
            int row = 0;
            while ((line = br.readLine()) != null) {
                row++;
                if (row == 1 && line.toLowerCase().contains("employeecode")) continue; // skip header
                batch.setTotalRows(batch.getTotalRows() + 1);

                try {
                    String[] cols = line.split(",", -1);
                    String code = cols[0].trim();
                    String dateStr = cols[2].trim();
                    String inStr = cols.length > 3 ? cols[3].trim() : "";
                    String outStr = cols.length > 4 ? cols[4].trim() : "";
                    String src = cols.length > 5 ? cols[5].trim() : "IMPORT_CSV";

                    var emp = employeeRepo.findAll().stream()
                            .filter(e -> e.getCode() != null && e.getCode().equalsIgnoreCase(code))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Employee not found: " + code));

                    LocalDate date = LocalDate.parse(dateStr);
                    LocalTime ci = inStr.isBlank() ? null : LocalTime.parse(inStr);
                    LocalTime co = outStr.isBlank() ? null : LocalTime.parse(outStr);

                    upsertRecord(batch.getId(), emp.getId(), date, ci, co, src);

                    batch.setSuccess(batch.getSuccess() + 1);
                } catch (Exception ex) {
                    batch.setFailed(batch.getFailed() + 1);
                    batch.getErrors().add("Row " + row + ": " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            batch.getErrors().add("FATAL: " + e.getMessage());
        }
        return batchRepo.save(batch);
    }

    /**
     * Upsert single attendance record
     */
    private void upsertRecord(String batchId, String empId, LocalDate date,
                              LocalTime ci, LocalTime co, String src) {

        var rec = attendanceRepo.findByEmployeeIdAndDate(empId, date)
                .orElseGet(() -> AttendanceRecord.builder()
                        .employeeId(empId).date(date).createdAt(Instant.now()).build());

        rec.setBatchId(batchId);
        rec.setCheckIn(ci);
        rec.setCheckOut(co);
        rec.setSource(src);

        // Apply rules theo schedule
        var schedule = scheduleService.getOrDefault();
        applyRules(rec, schedule);

        rec.setUpdatedAt(Instant.now());
        attendanceRepo.save(rec);
    }

    /**
     * Apply attendance rules: tính late/early/OT theo WorkSchedule
     * Public để controller có thể gọi khi quick edit
     */
    public AttendanceRecord applyRules(AttendanceRecord r, WorkSchedule s) {
        // 1. Check weekend/holiday
        boolean isWeekend = WEEKENDS.contains(r.getDate().getDayOfWeek());
        boolean isHoliday = !holidayRepo.findByDateBetween(r.getDate(), r.getDate()).isEmpty();

        if (isHoliday || isWeekend) {
            r.setStatus("HOLIDAY");
            r.setLateMinutes(0);
            r.setEarlyMinutes(0);
            r.setOtMinutes(0);
            r.setUpdatedAt(Instant.now());
            return r;
        }

        // 2. Nếu đã đánh dấu LEAVE thì không tính metrics
        if ("LEAVE".equalsIgnoreCase(r.getStatus())) {
            r.setLateMinutes(0);
            r.setEarlyMinutes(0);
            r.setOtMinutes(0);
            r.setUpdatedAt(Instant.now());
            return r;
        }

        // 3. Absent - không có check in/out
        if (r.getCheckIn() == null && r.getCheckOut() == null) {
            if (r.getStatus() == null || r.getStatus().isBlank()) {
                r.setStatus("ABSENT");
            }
            r.setLateMinutes(0);
            r.setEarlyMinutes(0);
            r.setOtMinutes(0);
            r.setUpdatedAt(Instant.now());
            return r;
        }

        // 4. Missing punch - chỉ có check in hoặc check out
        if (r.getCheckIn() == null || r.getCheckOut() == null) {
            if (r.getStatus() == null || r.getStatus().isBlank()) {
                r.setStatus("MISSING_PUNCH");
            }
            r.setLateMinutes(0);
            r.setEarlyMinutes(0);
            r.setOtMinutes(0);
            r.setUpdatedAt(Instant.now());
            return r;
        }

        // 5. Calculate metrics
        if (r.getStatus() == null || r.getStatus().isBlank()) {
            r.setStatus("PRESENT");
        }

        int late = 0, early = 0, ot = 0;

        // Late calculation
        if (r.getCheckIn() != null) {
            int diffMin = diffMinutes(s.getStartTime(), r.getCheckIn());
            int grace = Optional.ofNullable(s.getGraceLateMinutes()).orElse(0);
            if (diffMin > grace) {
                late = diffMin - grace;
            }
        }

        // Early & OT calculation
        if (r.getCheckOut() != null) {
            // Early leave
            int earlyMin = diffMinutes(r.getCheckOut(), s.getEndTime());
            int graceEarly = Optional.ofNullable(s.getGraceEarlyMinutes()).orElse(0);
            if (earlyMin > graceEarly) {
                early = earlyMin - graceEarly;
            }

            // OT
            int afterEnd = diffMinutes(s.getEndTime(), r.getCheckOut());
            int otAfter = Optional.ofNullable(s.getOtAfterMinutes()).orElse(0);
            if (afterEnd > otAfter) {
                ot = afterEnd - otAfter;
                ot = roundUp(ot, Optional.ofNullable(s.getOtRoundToMinutes()).orElse(1));
            }
        }

        r.setLateMinutes(Math.max(late, 0));
        r.setEarlyMinutes(Math.max(early, 0));
        r.setOtMinutes(Math.max(ot, 0));
        r.setUpdatedAt(Instant.now());

        return r;
    }

    /**
     * Get records for one employee in a month
     */
    public List<AttendanceRecord> recordsOf(String employeeId, YearMonth ym) {
        var from = ym.atDay(1);
        var to = ym.atEndOfMonth();
        return attendanceRepo.findByEmployeeIdAndDateBetween(employeeId, from, to);
    }

    /**
     * Get all company records for a month
     */
    public List<AttendanceRecord> companyRecords(YearMonth ym) {
        return attendanceRepo.findByDateBetween(ym.atDay(1), ym.atEndOfMonth());
    }

    // ===== Helper methods =====

    private int diffMinutes(LocalTime a, LocalTime b) {
        return (int) Duration.between(a, b).toMinutes();
    }

    private int roundUp(int minutes, int roundTo) {
        if (minutes <= 0 || roundTo <= 1) return Math.max(minutes, 0);
        int remainder = minutes % roundTo;
        return remainder == 0 ? minutes : minutes + (roundTo - remainder);
    }
}