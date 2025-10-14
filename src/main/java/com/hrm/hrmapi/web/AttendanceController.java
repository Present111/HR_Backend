// web/AttendanceController.java
package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.AttendanceBatch;
import com.hrm.hrmapi.domain.AttendanceRecord;
import com.hrm.hrmapi.domain.User;
import com.hrm.hrmapi.domain.WorkSchedule;
import com.hrm.hrmapi.repo.AttendanceRepo;
import com.hrm.hrmapi.repo.EmployeeRepo;
import com.hrm.hrmapi.service.AttendanceService;
import com.hrm.hrmapi.service.WorkScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService service;
    private final EmployeeRepo employees;

    // ---- thêm cho phần rule/quick edit ----
    private final WorkScheduleService scheduleService;
    private final AttendanceRepo attendanceRepo;

    // ---------------------------------------------------------
    // IMPORT CSV
    // ---------------------------------------------------------
    @Operation(summary = "Import bảng công theo tháng (CSV). Header: employeeCode,fullName,date,checkIn,checkOut,source")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceBatch importCsv(
            @RequestParam("month") @NotBlank String month,
            @RequestPart("file") MultipartFile file,
            Authentication auth
    ) {
        var me = (User) auth.getPrincipal();
        // đúng thứ tự: file trước, rồi YearMonth
        return service.importCsv(file, YearMonth.parse(month), me.getEmail());
    }

    // ---------------------------------------------------------
    // BẢNG CÔNG TOÀN CÔNG TY (để FE render matrix)
    // ---------------------------------------------------------
    @Operation(summary = "Bảng công toàn công ty theo tháng (để FE render matrix)")
    @GetMapping
    public Map<String, Object> company(
            @RequestParam("month") String month,
            @RequestParam(value = "department", required = false) String dept
    ) {
        var ym = YearMonth.parse(month);
        var records = service.companyRecords(ym);

        if (StringUtils.hasText(dept)) {
            Set<String> empIds = employees.findAll().stream()
                    .filter(e -> dept.equalsIgnoreCase(String.valueOf(e.getDepartment())))
                    .map(e -> e.getId())
                    .collect(Collectors.toSet());
            records = records.stream()
                    .filter(r -> empIds.contains(r.getEmployeeId()))
                    .toList();
        }

        return Map.of("month", month, "items", records);
    }

    // ---------------------------------------------------------
    // BẢNG CÔNG 1 NHÂN VIÊN TRONG THÁNG
    // ---------------------------------------------------------
    @Operation(summary = "Chi tiết bảng công 1 nhân viên trong tháng")
    @GetMapping("/{employeeId}")
    public Map<String, Object> byEmployee(
            @PathVariable String employeeId,
            @RequestParam("month") String month
    ) {
        var ym = YearMonth.parse(month);
        var items = service.recordsOf(employeeId, ym);
        return Map.of("employeeId", employeeId, "month", month, "items", items);
    }

    // =========================================================
    // PHẦN MỚI: CONFIG CA LÀM + QUICK EDIT + RECALC
    // =========================================================

    // ---- 1) Lấy/ghi cấu hình ca làm việc (ADMIN/MANAGER) ----
    @Operation(summary = "Lấy cấu hình ca làm việc (giờ chuẩn, grace, OT...)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/schedule")
    public WorkSchedule getSchedule() {
        return scheduleService.getOrDefault();
    }

    @Operation(summary = "Cập nhật cấu hình ca làm việc (giờ chuẩn, grace, OT...)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/schedule")
    public WorkSchedule putSchedule(@RequestBody WorkSchedule body) {
        return scheduleService.upsert(body);
    }

    // ---- 2) Sửa nhanh 1 record (ADMIN/MANAGER) ----
    @Data
    static class PatchAttendanceReq {
        private String checkIn;   // "HH:mm" hoặc null/empty để clear
        private String checkOut;  // "HH:mm" hoặc null/empty để clear
        private String status;    // PRESENT/LEAVE/ABSENT/WFH...
        private String note;
    }

    @Operation(summary = "Chỉnh sửa nhanh 1 bản ghi; tự tính lại late/early/OT")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PatchMapping("/{id}")
    public AttendanceRecord patchRecord(@PathVariable String id, @RequestBody PatchAttendanceReq body) {
        var rec = attendanceRepo.findById(id).orElseThrow();

        if (body.getCheckIn() != null) {
            rec.setCheckIn(parseTimeOrNull(body.getCheckIn()));
        }
        if (body.getCheckOut() != null) {
            rec.setCheckOut(parseTimeOrNull(body.getCheckOut()));
        }
        if (StringUtils.hasText(body.getStatus())) {
            rec.setStatus(body.getStatus());
        }
        if (body.getNote() != null) {
            rec.setNote(body.getNote());
        }

        var schedule = scheduleService.getOrDefault();
        service.applyRules(rec, schedule); // tự tính late/early/ot
        return attendanceRepo.save(rec);
    }

    // ---- 3) Recalc cả tháng (ADMIN/MANAGER) ----
    @Operation(summary = "Recalc lại toàn bộ late/early/OT theo tháng (optional: theo employeeId)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/recalc")
    public Map<String, Object> recalc(
            @RequestParam("month") String month,
            @RequestParam(value = "employeeId", required = false) String employeeId
    ) {
        var ym = YearMonth.parse(month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        var list = (StringUtils.hasText(employeeId))
                ? attendanceRepo.findByEmployeeIdAndDateBetween(employeeId, from, to)
                : attendanceRepo.findByDateBetween(from, to);

        var schedule = scheduleService.getOrDefault();
        int count = 0;
        for (var r : list) {
            service.applyRules(r, schedule);
            attendanceRepo.save(r);
            count++;
        }
        return Map.of("month", month, "recalculated", count);
    }

    // ---------------------------------------------------------
    // helpers
    // ---------------------------------------------------------
    private LocalTime parseTimeOrNull(String s) {
        if (!StringUtils.hasText(s)) return null;
        return LocalTime.parse(s);
    }
}
