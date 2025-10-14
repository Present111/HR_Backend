// web/AttendanceController.java
package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.AttendanceBatch;
import com.hrm.hrmapi.domain.User;
import com.hrm.hrmapi.repo.EmployeeRepo;
import com.hrm.hrmapi.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        // ĐÚNG THỨ TỰ: file trước, rồi YearMonth
        return service.importCsv(file, YearMonth.parse(month), me.getEmail());
    }

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
}
