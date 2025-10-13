// web/EmployeesController.java
package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.Employee;
import com.hrm.hrmapi.domain.User;
import com.hrm.hrmapi.repo.ContractRepo;
import com.hrm.hrmapi.repo.EmployeeRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Employees")
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeesController {
    private final EmployeeRepo employees;
    private final ContractRepo contracts;

    @Operation(summary = "Danh sách nhân viên (ADMIN/MANAGER)")
    @GetMapping
    public List<Employee> list() { return employees.findAll(); }

    @Operation(summary = "Chi tiết nhân viên; EMPLOYEE chỉ xem hồ sơ của chính mình")
    @GetMapping("/{id}")
    public Employee one(@PathVariable String id, Authentication auth){
        var me = (User) auth.getPrincipal();
        if (me.getRole() == User.Role.EMPLOYEE && !id.equals(me.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return employees.findById(id).orElseThrow();
    }

    @Operation(summary = "Danh sách hợp đồng theo nhân viên")
    @GetMapping("/{id}/contracts")
    public Object empContracts(@PathVariable String id){
        return contracts.findByEmployeeIdOrderByVersionDesc(id);
    }

    /* ========= Tạo mới nhân viên (ADMIN/MANAGER) ========= */

    public record CreateEmployeeRequest(
            @Schema(example = "NV-2025-0100") @NotBlank @Size(max = 50) String code,
            @Schema(example = "Phạm Hoài Nam") @NotBlank @Size(max = 120) String fullName,
            @Schema(example = "IT") @NotBlank @Size(max = 60) String department,
            @Schema(example = "Frontend Dev") @NotBlank @Size(max = 80) String position,
            @Schema(example = "ACTIVE") @NotBlank String status,
            @Schema(example = "2024-08-20") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDate,
            @Schema(example = "0902345678") String phone,
            @Schema(example = "Q.7, HCM") String address
    ) {}

    @Operation(
            summary = "Tạo nhân viên (chỉ ADMIN/MANAGER)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = CreateEmployeeRequest.class),
                            examples = @ExampleObject(value = """
                {
                  "code": "NV-2025-0100",
                  "fullName": "Phạm Hoài Nam",
                  "department": "IT",
                  "position": "Frontend Dev",
                  "status": "ACTIVE",
                  "joinDate": "2024-08-20",
                  "phone": "0902345678",
                  "address": "Q.7, HCM"
                }
              """)
                    )
            )
    )
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Employee create(@Valid @RequestBody CreateEmployeeRequest req) {
        // Có thể kiểm tra trùng code nếu muốn
        employees.findAll().stream()
                .filter(e -> e.getCode() != null && e.getCode().equalsIgnoreCase(req.code()))
                .findAny()
                .ifPresent(e -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee code already exists"); });

        var emp = Employee.builder()
                .code(req.code())
                .fullName(req.fullName())
                .department(req.department())
                .position(req.position())
                .status(req.status())
                .joinDate(req.joinDate() != null ? req.joinDate() : LocalDate.now())
                .phone(req.phone())
                .address(req.address())
                .build();

        return employees.save(emp);
    }
}
