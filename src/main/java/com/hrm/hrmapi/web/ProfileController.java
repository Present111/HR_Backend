package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.Employee;
import com.hrm.hrmapi.domain.User;
import com.hrm.hrmapi.repo.EmployeeRepo;
import com.hrm.hrmapi.repo.UserRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "My Profile")
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepo users;
    private final EmployeeRepo employees;

    // ----- GET: xem thông tin cá nhân + quick summary -----
    @Operation(summary = "Xem thông tin cá nhân + quick summary")
    @GetMapping
    public Map<String, Object> myProfile(Authentication auth) {
        var u = (User) auth.getPrincipal();

        Employee emp = null;
        if (u.getEmployeeId() != null) {
            emp = employees.findById(u.getEmployeeId()).orElse(null);
        }

        var res = new LinkedHashMap<String, Object>();

        // ---- user map (allow nulls) ----
        var userMap = new LinkedHashMap<String, Object>();
        userMap.put("id", u.getId());
        userMap.put("email", u.getEmail());
        userMap.put("fullName", u.getFullName());
        userMap.put("role", u.getRole() != null ? u.getRole().name() : null);
        userMap.put("avatarUrl", u.getAvatarUrl());
        userMap.put("employeeId", u.getEmployeeId());
        res.put("user", userMap);

        // ---- employee map (allow nulls) ----
        if (emp == null) {
            res.put("employee", null);
        } else {
            var empMap = new LinkedHashMap<String, Object>();
            empMap.put("id", emp.getId());
            empMap.put("code", emp.getCode());
            empMap.put("fullName", emp.getFullName());
            empMap.put("department", emp.getDepartment());
            empMap.put("position", emp.getPosition());
            empMap.put("status", emp.getStatus());
            empMap.put("joinDate", emp.getJoinDate());
            empMap.put("phone", emp.getPhone());
            empMap.put("address", emp.getAddress());
            empMap.put("emergencyContact", emp.getEmergencyContact());
            res.put("employee", empMap);
        }

        // ---- quick summary (mock) ----
        var summary = new LinkedHashMap<String, Object>();
        summary.put("leaveRemaining", 12);
        summary.put("assetsInUse", 0);
        summary.put("latestPayslipMonth", YearMonth.now().minusMonths(1).toString());
        res.put("summary", summary);

        return res;
    }

    // ----- PUT: cập nhật profile cơ bản -----
    public record UpdateProfileRequest(
            String fullName,                // cập nhật vào User và/hoặc Employee
            String phone,
            String address,
            Emergency emergency
    ) {
        public record Emergency(
                @NotBlank String name,
                @NotBlank String phone,
                @NotBlank String relation
        ) {}
    }

    @Operation(summary = "Cập nhật thông tin cá nhân (điện thoại, địa chỉ, liên hệ khẩn)")
    @PutMapping
    public Map<String, Object> update(Authentication auth,
                                      @RequestBody @Valid UpdateProfileRequest req) {
        var u = (User) auth.getPrincipal();
        if (req.fullName() != null && !req.fullName().isBlank()) {
            u.setFullName(req.fullName());
            users.save(u);
        }

        if (u.getEmployeeId() != null) {
            var emp = employees.findById(u.getEmployeeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

            if (req.fullName() != null && !req.fullName().isBlank()) emp.setFullName(req.fullName());
            if (req.phone() != null) emp.setPhone(req.phone());
            if (req.address() != null) emp.setAddress(req.address());
            if (req.emergency() != null) {
                var e = new Employee.Emergency();
                e.setName(req.emergency().name());
                e.setPhone(req.emergency().phone());
                e.setRelation(req.emergency().relation());
                emp.setEmergencyContact(e);
            }
            employees.save(emp);
        }

        return Map.of("message", "Profile updated");
    }

    // ----- PUT: cập nhật avatar (mock: nhận URL) -----
    public record UpdateAvatarRequest(@NotBlank String avatarUrl) {}

    @Operation(summary = "Cập nhật avatar (mock: truyền URL)")
    @PutMapping("/avatar")
    public Map<String, Object> updateAvatar(Authentication auth,
                                            @RequestBody @Valid UpdateAvatarRequest body) {
        var u = (User) auth.getPrincipal();
        u.setAvatarUrl(body.avatarUrl());
        users.save(u);
        return Map.of("message", "Avatar updated", "avatarUrl", u.getAvatarUrl());
    }
}
