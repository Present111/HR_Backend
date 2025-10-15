// src/main/java/com/hrm/hrmapi/service/AttendanceSummaryService.java
package com.hrm.hrmapi.service;

import com.hrm.hrmapi.domain.AttendanceRecord;
import com.hrm.hrmapi.domain.LeaveRequest;
import com.hrm.hrmapi.domain.LeaveType;
import com.hrm.hrmapi.payroll.AttendanceSummary;
import com.hrm.hrmapi.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceSummaryService {

    private final AttendanceRepo attendanceRepo;
    private final LeaveRequestRepo leaveRequestRepo;
    private final HolidayRepo holidayRepo;
    private final ContractRepo contractRepo;
    private final LeaveTypeRepo leaveTypeRepo;

    public AttendanceSummary summarize(String employeeId, LocalDate start, LocalDate end) {
        // lấy ngày giữa kỳ, an toàn hơn so với dùng start hoặc end
        LocalDate mid = start.plusDays((int) ((end.toEpochDay() - start.toEpochDay()) / 2));

        var contract = contractRepo.findActiveByEmployee(employeeId, mid)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy hợp đồng đang hiệu lực"));



        var holidays = holidayRepo.findByDateBetween(start, end);
        int workingDaysInCycle = DateUtils.countWorkingDays(start, end, holidays);

        List<AttendanceRecord> records =
                attendanceRepo.findByEmployeeIdAndDateBetween(employeeId, start, end);

        int late = records.stream().mapToInt(r -> nvl(r.getLateMinutes())).sum();
        int early = records.stream().mapToInt(r -> nvl(r.getEarlyMinutes())).sum();

        var holidayDates = holidays.stream().map(h -> h.getDate()).collect(Collectors.toSet());
        int otWeekday = 0, otWeekend = 0, otHoliday = 0;
        for (var r : records) {
            int m = nvl(r.getOtMinutes());
            LocalDate d = r.getDate();
            if (holidayDates.contains(d)) otHoliday += m;
            else if (DateUtils.isWeekend(d)) otWeekend += m;
            else otWeekday += m;
        }

        // tập hợp các loại nghỉ không lương
        Set<String> unpaidTypeIds = leaveTypeRepo.findAll().stream()
                .filter(this::isUnpaidType)
                .map(LeaveType::getId)
                .collect(Collectors.toSet());

        // đơn đã duyệt trong kỳ
        List<LeaveRequest> approved = leaveRequestRepo
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employeeId, "APPROVED", end, start);

        int unpaidLeaveDays = 0;
        for (var lr : approved) {
            boolean unpaid = false;

            // 1) thử lấy embedded LeaveType trong đơn
            LeaveType embedded = tryGetEmbeddedLeaveType(lr);
            if (embedded != null) unpaid = isUnpaidType(embedded);

            // 2) nếu chưa xác định được, thử lấy id của leave type
            if (!unpaid) {
                String typeId = tryGetLeaveTypeId(lr);
                if (typeId != null) unpaid = unpaidTypeIds.contains(typeId);
            }

            if (!unpaid) continue;

            LocalDate s = lr.getStartDate().isBefore(start) ? start : lr.getStartDate();
            LocalDate e = lr.getEndDate().isAfter(end) ? end : lr.getEndDate();
            for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                if (!DateUtils.isWeekend(d) && !holidayDates.contains(d)) {
                    unpaidLeaveDays++;
                }
            }
        }

        int workingDaysPaid = Math.max(0, workingDaysInCycle - unpaidLeaveDays);

        BigDecimal baseHourly = contract.getBaseSalary()
                .divide(BigDecimal.valueOf(workingDaysInCycle * 8.0), 2, java.math.RoundingMode.HALF_UP);

        return AttendanceSummary.builder()
                .workingDaysPaid(workingDaysPaid)
                .workingDaysInCycle(workingDaysInCycle)
                .unpaidLeaveDays(unpaidLeaveDays)
                .lateMinutes(late)
                .earlyLeaveMinutes(early)
                .otMinutesWeekday(otWeekday)
                .otMinutesWeekend(otWeekend)
                .otMinutesHoliday(otHoliday)
                .baseSalary(contract.getBaseSalary())
                .baseHourly(baseHourly)
                .build();
    }

    private int nvl(Integer x) { return x == null ? 0 : x; }

    /* ====== helpers không phụ thuộc tên field/method cụ thể ====== */

    /** cố gắng lấy LeaveType embed trong LeaveRequest nếu có */
    private LeaveType tryGetEmbeddedLeaveType(LeaveRequest lr) {
        // thử getter getLeaveType()
        try {
            Method m = lr.getClass().getMethod("getLeaveType");
            Object v = m.invoke(lr);
            if (v instanceof LeaveType) return (LeaveType) v;
        } catch (Exception ignored) {}
        // thử getter getType()
        try {
            Method m = lr.getClass().getMethod("getType");
            Object v = m.invoke(lr);
            if (v instanceof LeaveType) return (LeaveType) v;
        } catch (Exception ignored) {}
        // thử field leaveType
        try {
            Field f = lr.getClass().getDeclaredField("leaveType");
            f.setAccessible(true);
            Object v = f.get(lr);
            if (v instanceof LeaveType) return (LeaveType) v;
        } catch (Exception ignored) {}
        // thử field type
        try {
            Field f = lr.getClass().getDeclaredField("type");
            f.setAccessible(true);
            Object v = f.get(lr);
            if (v instanceof LeaveType) return (LeaveType) v;
        } catch (Exception ignored) {}
        return null;
    }

    /** cố gắng lấy id của LeaveType từ LeaveRequest nếu nó chỉ lưu id */
    private String tryGetLeaveTypeId(LeaveRequest lr) {
        for (String name : new String[]{"getLeaveTypeId", "getTypeId", "getLeaveTypeCode", "getLeaveType", "getType"}) {
            try {
                Method m = lr.getClass().getMethod(name);
                Object v = m.invoke(lr);
                if (v instanceof String) return (String) v;
            } catch (Exception ignored) {}
        }
        for (String field : new String[]{"leaveTypeId", "typeId", "leaveTypeCode"}) {
            try {
                Field f = lr.getClass().getDeclaredField(field);
                f.setAccessible(true);
                Object v = f.get(lr);
                if (v instanceof String) return (String) v;
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** xác định LeaveType là không lương */
    private boolean isUnpaidType(LeaveType t) {
        if (t == null) return false;
        // isPaid()/getPaid()/field paid
        try { Method m = t.getClass().getMethod("isPaid"); Object v = m.invoke(t); if (v instanceof Boolean) return !((Boolean) v); } catch (Exception ignored) {}
        try { Method m = t.getClass().getMethod("getPaid"); Object v = m.invoke(t); if (v instanceof Boolean) return !((Boolean) v); } catch (Exception ignored) {}
        try { Field f = t.getClass().getDeclaredField("paid"); f.setAccessible(true); Object v = f.get(t); if (v instanceof Boolean) return !((Boolean) v); } catch (Exception ignored) {}
        // fallback theo code/name
        try { Method m = t.getClass().getMethod("getCode"); Object v = m.invoke(t); if (v != null && v.toString().toLowerCase().contains("unpaid")) return true; } catch (Exception ignored) {}
        try {
            Method m = t.getClass().getMethod("getName");
            Object v = m.invoke(t);
            if (v != null) {
                String s = v.toString().toLowerCase();
                if (s.contains("unpaid") || s.contains("không lương") || s.contains("khong luong")) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
