package com.hrm.hrmapi.service;

import com.hrm.hrmapi.domain.*;
import com.hrm.hrmapi.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepo leaveRepo;
    private final LeaveQuotaRepo quotaRepo;
    private final LeaveTypeRepo typeRepo;
    private final HolidayRepo holidayRepo;
    private final AttendanceRepo attendanceRepo;

    // session weight
    private static final Map<String, Double> SESSION = Map.of(
            "AM", 0.5, "PM", 0.5, "FULL", 1.0
    );
    private static final Set<DayOfWeek> WEEKENDS = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public LeaveQuota quotaOf(String employeeId, int year) {
        return quotaRepo.findByEmployeeIdAndYear(employeeId, year)
                .orElseGet(() -> quotaRepo.save(LeaveQuota.builder()
                        .employeeId(employeeId).year(year)
                        .entitlement(12).carriedOver(0).taken(0).remaining(12)
                        .lastCalculatedAt(java.time.Instant.now())
                        .build()));
    }

    public LeaveRequest create(LeaveRequest req, String creator) {
        var type = typeRepo.findByCodeIgnoreCase(req.getTypeCode())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unknown leave type"));

        double days = computeDays(req.getStartDate(), req.getStartSession(),
                req.getEndDate(), req.getEndSession());

        req.setDays(days);
        req.setStatus("PENDING");
        req.setCreatedBy(creator);
        req.setCreatedAt(java.time.Instant.now());
        req.setUpdatedAt(req.getCreatedAt());
        return leaveRepo.save(req);
    }

    /** PENDING -> APPROVED, trừ quota nếu cần, tạo/điều chỉnh attendance status=LEAVE */
    public LeaveRequest approve(String id, String approverId) {
        var req = leaveRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Leave not found"));

        if (!"PENDING".equals(req.getStatus()))
            throw new ResponseStatusException(BAD_REQUEST, "Only PENDING can be approved");

        var type = typeRepo.findByCodeIgnoreCase(req.getTypeCode())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unknown type"));

        // update quota
        if (type.isDeductQuota()) {
            var q = quotaOf(req.getEmployeeId(), req.getStartDate().getYear());
            q.setTaken(q.getTaken() + req.getDays());
            q.setRemaining(q.getEntitlement() + q.getCarriedOver() - q.getTaken());
            q.setLastCalculatedAt(java.time.Instant.now());
            quotaRepo.save(q);
        }

        // mark attendance
        markAttendanceLeave(req);

        req.setStatus("APPROVED");
        req.setApproverId(approverId);
        req.setUpdatedAt(java.time.Instant.now());
        return leaveRepo.save(req);
    }

    public LeaveRequest reject(String id, String approverId, String note) {
        var req = leaveRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Leave not found"));

        if (!"PENDING".equals(req.getStatus()))
            throw new ResponseStatusException(BAD_REQUEST, "Only PENDING can be rejected");

        req.setStatus("REJECTED");
        req.setApproverId(approverId);
        req.setManagerNote(note);
        req.setUpdatedAt(java.time.Instant.now());
        return leaveRepo.save(req);
    }

    /** Tính tổng ngày nghỉ (bỏ weekend/holiday, tính AM/PM) */
    public double computeDays(LocalDate start, String startSession,
                              LocalDate end, String endSession) {
        var holidays = holidayRepo.findByDateBetween(start, end)
                .stream().map(Holiday::getDate).collect(Collectors.toSet());

        double sum = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (!WEEKENDS.contains(d.getDayOfWeek()) && !holidays.contains(d)) {
                if (d.equals(start) && d.equals(end)) {
                    // cùng 1 ngày
                    sum += sessionWeight(startSession, endSession);
                } else if (d.equals(start)) {
                    sum += SESSION.getOrDefault(startSession, 1.0);
                } else if (d.equals(end)) {
                    sum += SESSION.getOrDefault(endSession, 1.0);
                } else {
                    sum += 1.0;
                }
            }
            d = d.plusDays(1);
        }
        return sum;
    }

    public List<LeaveRequest> list(String status, YearMonth month) {
        if (status != null && !status.isBlank()) {
            return leaveRepo.findByStatus(status);
        }
        if (month != null) {
            return leaveRepo.findByStartDateBetween(month.atDay(1), month.atEndOfMonth());
        }
        return leaveRepo.findAll();
    }

    private double sessionWeight(String startSession, String endSession) {
        if ("FULL".equals(startSession) && "FULL".equals(endSession)) return 1.0;
        if ("AM".equals(startSession) && "AM".equals(endSession)) return 0.5;
        if ("PM".equals(startSession) && "PM".equals(endSession)) return 0.5;
        if ("AM".equals(startSession) && "PM".equals(endSession)) return 1.0;
        if ("FULL".equals(startSession) && "AM".equals(endSession)) return 0.5;
        if ("FULL".equals(startSession) && "PM".equals(endSession)) return 0.5;
        return 1.0;
    }

    /** Ghi/ghi đè AttendanceRecord status=LEAVE cho các ngày đã duyệt */
    private void markAttendanceLeave(LeaveRequest r) {
        LocalDate d = r.getStartDate();
        while (!d.isAfter(r.getEndDate())) {
            AttendanceRecord rec = attendanceRepo
                    .findByEmployeeIdAndDate(r.getEmployeeId(), d)
                    .orElse(null);

            if (rec == null) {
                rec = AttendanceRecord.builder()
                        .employeeId(r.getEmployeeId())
                        .date(d)
                        .createdAt(java.time.Instant.now())
                        .build();
            }

            rec.setStatus("LEAVE");
            rec.setCheckIn(null);
            rec.setCheckOut(null);
            rec.setLateMinutes(0);
            rec.setEarlyMinutes(0);
            rec.setOtMinutes(0);
            rec.setUpdatedAt(java.time.Instant.now());

            attendanceRepo.save(rec);
            d = d.plusDays(1);
        }
    }
}
