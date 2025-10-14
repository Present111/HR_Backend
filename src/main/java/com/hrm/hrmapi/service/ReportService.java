package com.hrm.hrmapi.service;

import com.hrm.hrmapi.domain.AttendanceRecord;
import com.hrm.hrmapi.domain.Employee;
import com.hrm.hrmapi.domain.LeaveRequest;
import com.hrm.hrmapi.repo.AttendanceRepo;
import com.hrm.hrmapi.repo.EmployeeRepo;
import com.hrm.hrmapi.repo.LeaveRequestRepo;
import com.hrm.hrmapi.web.dto.report.AttendanceMonthlySummary;
import com.hrm.hrmapi.web.dto.report.AttendanceReport;
import com.hrm.hrmapi.web.dto.report.AttendanceRow;
import com.hrm.hrmapi.web.dto.report.LeaveReport;
import com.hrm.hrmapi.web.dto.report.LeaveRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AttendanceRepo attendanceRepo;
    private final EmployeeRepo employeeRepo;
    private final LeaveRequestRepo leaveRepo;

    public AttendanceReport buildAttendanceReport(String month, String department) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<AttendanceRecord> atts = attendanceRepo.findByDateBetween(start, end);

        if (department != null && !department.isBlank()) {
            // dùng đúng method hiện có
            List<Employee> deptEmps = employeeRepo.findByDepartment(department);
            Set<String> empIds = deptEmps.stream().map(Employee::getId).collect(Collectors.toSet());
            atts = atts.stream().filter(a -> empIds.contains(a.getEmployeeId())).toList();
        }

        Map<String, Employee> empMap = employeeRepo.findAll()
                .stream().collect(Collectors.toMap(Employee::getId, e -> e));

        List<AttendanceRow> rows = new ArrayList<>();
        int late = 0, early = 0, ot = 0;
        int present = 0, leave = 0, wfh = 0, holiday = 0, absent = 0;

        for (AttendanceRecord a : atts) {
            Employee emp = empMap.get(a.getEmployeeId());
            String name = (emp != null && emp.getFullName() != null) ? emp.getFullName() : a.getEmployeeId();
            String dept = (emp != null) ? Optional.ofNullable(emp.getDepartment()).orElse("") : "";


            int lm = Optional.ofNullable(a.getLateMinutes()).orElse(0);
            int em = Optional.ofNullable(a.getEarlyMinutes()).orElse(0);
            int om = Optional.ofNullable(a.getOtMinutes()).orElse(0);
            String st = Optional.ofNullable(a.getStatus()).orElse("PRESENT");

            rows.add(AttendanceRow.builder()
                    .employeeId(a.getEmployeeId())
                    .employeeName(name)
                    .department(dept)
                    .date(a.getDate() != null ? a.getDate().toString() : "")
                    .status(st)
                    .checkIn(a.getCheckIn() != null ? a.getCheckIn().toString() : "")
                    .checkOut(a.getCheckOut() != null ? a.getCheckOut().toString() : "")
                    .lateMinutes(lm)
                    .earlyMinutes(em)
                    .otMinutes(om)
                    .build());

            late += lm; early += em; ot += om;
            switch (st) {
                case "LEAVE" -> leave++;
                case "WFH" -> wfh++;
                case "HOLIDAY" -> holiday++;
                case "ABSENT" -> absent++;
                default -> present++;
            }
        }

        rows.sort(Comparator.comparing(AttendanceRow::getDepartment)
                .thenComparing(AttendanceRow::getEmployeeName)
                .thenComparing(AttendanceRow::getDate));

        AttendanceMonthlySummary sum = AttendanceMonthlySummary.builder()
                .totalLateMinutes(late)
                .totalEarlyMinutes(early)
                .totalOtMinutes(ot)
                .totalPresentDays(present)
                .totalLeaveDays(leave)
                .totalWfhDays(wfh)
                .totalHolidayDays(holiday)
                .totalAbsentDays(absent)
                .build();

        return AttendanceReport.builder()
                .month(month)
                .department(department)
                .rows(rows)
                .summary(sum)
                .build();
    }

    public LeaveReport buildLeaveReport(String month, String department) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // Dùng method sẵn có trong repo
        List<LeaveRequest> leaves = leaveRepo.findByStartDateBetween(start, end);

        if (department != null && !department.isBlank()) {
            List<Employee> deptEmps = employeeRepo.findByDepartment(department);
            Set<String> empIds = deptEmps.stream().map(Employee::getId).collect(Collectors.toSet());
            leaves = leaves.stream().filter(l -> empIds.contains(l.getEmployeeId())).toList();
        }

        Map<String, Employee> empMap = employeeRepo.findAll()
                .stream().collect(Collectors.toMap(Employee::getId, e -> e));

        List<LeaveRow> rows = new ArrayList<>();
        double approved = 0.0, pending = 0.0;

        for (LeaveRequest l : leaves) {
            Employee emp = empMap.get(l.getEmployeeId());
            String name = (emp != null && emp.getFullName() != null) ? emp.getFullName() : l.getEmployeeId();
            String dept = (emp != null) ? Optional.ofNullable(emp.getDepartment()).orElse("") : "";
            double days = l.getDays(); // dùng đúng field trong LeaveRequest

            rows.add(LeaveRow.builder()
                    .requestId(l.getId())
                    .employeeId(l.getEmployeeId())
                    .employeeName(name)
                    .department(dept)
                    .type(l.getTypeCode()) // đổi từ getType() -> getTypeCode()
                    .fromDate(l.getStartDate() != null ? l.getStartDate().toString() : "")
                    .toDate(l.getEndDate() != null ? l.getEndDate().toString() : "")
                    .days(days)
                    .status(l.getStatus()) // status là String
                    .build());

            if ("APPROVED".equals(l.getStatus())) approved += days;
            if ("PENDING".equals(l.getStatus()))  pending += days;
        }

        rows.sort(Comparator.comparing(LeaveRow::getDepartment)
                .thenComparing(LeaveRow::getEmployeeName)
                .thenComparing(LeaveRow::getFromDate));

        return LeaveReport.builder()
                .month(month)
                .department(department)
                .rows(rows)
                .totalDaysApproved(approved)
                .totalDaysPending(pending)
                .build();
    }

}
