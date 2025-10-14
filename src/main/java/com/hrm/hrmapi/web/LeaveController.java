package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.LeaveQuota;
import com.hrm.hrmapi.domain.LeaveRequest;
import com.hrm.hrmapi.domain.User;
import com.hrm.hrmapi.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/leave")
public class LeaveController {

    private final LeaveService service;

    @Operation(summary = "Xem quota năm của 1 nhân viên")
    @GetMapping("/quota/{employeeId}")
    public LeaveQuota quota(@PathVariable String employeeId,
                            @RequestParam("year") Integer year) {
        return service.quotaOf(employeeId, year);
    }

    @Operation(summary = "Employee tạo đơn nghỉ")
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequest create(@Valid @RequestBody LeaveRequest body, Authentication auth) {
        var me = (User) auth.getPrincipal();
        return service.create(body, me.getEmail());
    }

    @Operation(summary = "Danh sách đơn nghỉ (lọc PENDING hoặc theo khoảng tháng)")
    @GetMapping("/requests")
    public List<LeaveRequest> list(@RequestParam(value = "status", required = false) String status,
                                   @RequestParam(value = "month", required = false) String month) {
        YearMonth ym = (month != null && !month.isBlank()) ? YearMonth.parse(month) : null;
        return service.list(status, ym);
    }

    @Operation(summary = "Manager approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/requests/{id}/approve")
    public LeaveRequest approve(@PathVariable String id, Authentication auth) {
        var me = (User) auth.getPrincipal();
        return service.approve(id, me.getId());
    }

    @Operation(summary = "Manager reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/requests/{id}/reject")
    public LeaveRequest reject(@PathVariable String id, @RequestParam(required = false) String note,
                               Authentication auth) {
        var me = (User) auth.getPrincipal();
        return service.reject(id, me.getId(), note);
    }
}
