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
import java.util.stream.Collectors;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepo attendanceRepo;
    private final AttendanceBatchRepo batchRepo;
    private final EmployeeRepo employeeRepo;
    private final HolidayRepo holidayRepo;

    // RULES (bạn chỉnh tuỳ policy)
    private static final LocalTime SHIFT_IN = LocalTime.of(9, 0);
    private static final LocalTime SHIFT_OUT = LocalTime.of(18, 0);
    private static final int GRACE_MINUTES = 5;      // trễ cho phép
    private static final int OT_THRESHOLD_MIN = 0;   // bắt đầu tính OT sau x phút
    private static final Set<DayOfWeek> WEEKENDS = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public AttendanceBatch importCsv(MultipartFile file, YearMonth ym, String importedBy) {
        var batch = AttendanceBatch.builder()
                .month(ym.toString())
                .filename(file.getOriginalFilename())
                .importedBy(importedBy)
                .importedAt(java.time.Instant.now())
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

    private void upsertRecord(String batchId, String empId, LocalDate date, LocalTime ci, LocalTime co, String src) {
        var rec = attendanceRepo.findByEmployeeIdAndDate(empId, date)
                .orElseGet(() -> AttendanceRecord.builder()
                        .employeeId(empId).date(date).createdAt(java.time.Instant.now()).build());

        rec.setBatchId(batchId);
        rec.setCheckIn(ci);
        rec.setCheckOut(co);
        rec.setSource(src);
        applyStatusAndMetrics(rec);
        rec.setUpdatedAt(java.time.Instant.now());
        attendanceRepo.save(rec);
    }

    /** Gán status + late/early/ot dựa trên rule đơn giản + holiday/weekend */
    public void applyStatusAndMetrics(AttendanceRecord r) {
        // holiday/weekend?
        boolean isWeekend = WEEKENDS.contains(r.getDate().getDayOfWeek());
        boolean isHoliday = !holidayRepo.findByDateBetween(r.getDate(), r.getDate()).isEmpty();

        if (isHoliday || isWeekend) {
            r.setStatus("HOLIDAY");
            r.setLateMinutes(0); r.setEarlyMinutes(0); r.setOtMinutes(0);
            return;
        }

        if (r.getCheckIn() == null && r.getCheckOut() == null) {
            // có thể sau này set thành ABSENT nếu không có leave
            r.setStatus("ABSENT");
            r.setLateMinutes(null); r.setEarlyMinutes(null); r.setOtMinutes(null);
            return;
        }

        if (r.getCheckIn() == null || r.getCheckOut() == null) {
            r.setStatus("MISSING_PUNCH");
            r.setLateMinutes(null); r.setEarlyMinutes(null); r.setOtMinutes(null);
            return;
        }

        // OK: tính trễ/sớm/OT
        int late = Math.max(0, (int) Duration.between(SHIFT_IN.plusMinutes(GRACE_MINUTES), r.getCheckIn()).toMinutes());
        int early = Math.max(0, (int) Duration.between(r.getCheckOut(), SHIFT_OUT).toMinutes());
        int ot = Math.max(0, (int) Duration.between(SHIFT_OUT.plusMinutes(OT_THRESHOLD_MIN), r.getCheckOut()).toMinutes());

        r.setStatus("OK");
        r.setLateMinutes(late);
        r.setEarlyMinutes(early);
        r.setOtMinutes(ot);
    }

    /** Trả list records tháng cho 1 employee */
    public List<AttendanceRecord> recordsOf(String employeeId, YearMonth ym) {
        var from = ym.atDay(1);
        var to = ym.atEndOfMonth();
        return attendanceRepo.findByEmployeeIdAndDateBetween(employeeId, from, to);
    }

    /** Toàn công ty theo tháng (đổ ra FE vẽ matrix) */
    public List<AttendanceRecord> companyRecords(YearMonth ym) {
        return attendanceRepo.findByDateBetween(ym.atDay(1), ym.atEndOfMonth());
    }
}
