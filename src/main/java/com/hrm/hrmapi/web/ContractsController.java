// web/ContractsController.java
package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.Contract;
import com.hrm.hrmapi.domain.Employee;
import com.hrm.hrmapi.domain.User;
import com.hrm.hrmapi.repo.ContractRepo;
import com.hrm.hrmapi.repo.EmployeeRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Tag(name = "Contracts")
@RestController
@RequiredArgsConstructor
@RequestMapping("/contracts") // <<< prefix cố định để tránh trùng với EmployeesController
public class ContractsController {

    private final ContractRepo contracts;
    private final EmployeeRepo employees;

    /* ===================== DTOs ===================== */

    public record CreateContractRequest(
            @Schema(example = "LABOR", description = "Loại HĐ: LABOR | SERVICE | PROBATION ...")
            @NotBlank String type,
            @Schema(example = "2024-09-01") @NotNull LocalDate startDate,
            @Schema(example = "2025-08-31") LocalDate endDate,
            @Schema(example = "ACTIVE", description = "ACTIVE | EXPIRED") String status,
            @Schema(example = "18000000") @NotNull BigDecimal baseSalary
    ) {}

    public record UpdateContractRequest(
            @Schema(example = "LABOR") String type,
            @Schema(example = "2024-09-01") LocalDate startDate,
            @Schema(example = "2025-08-31") LocalDate endDate,
            @Schema(example = "EXPIRED") String status,
            @Schema(example = "20000000") BigDecimal baseSalary
    ) {}

    /* ===================== READ ===================== */

    @Operation(summary = "Danh sách hợp đồng theo nhân viên (version desc). EMPLOYEE chỉ xem của mình.")
    @GetMapping("/by-employee/{employeeId}")
    public List<Contract> byEmployee(@PathVariable String employeeId, Authentication auth) {
        authorizeReadOwn(employeeId, auth);
        return contracts.findByEmployeeIdOrderByVersionDesc(employeeId);
    }

    @Operation(summary = "Chi tiết hợp đồng")
    @GetMapping("/{id}")
    public Contract one(@PathVariable String id, Authentication auth) {
        var c = contracts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found"));
        authorizeReadOwn(c.getEmployeeId(), auth);
        return c;
    }

    /* ===================== CREATE ===================== */

    @Operation(
            summary = "Tạo hợp đồng mới cho nhân viên (ADMIN/MANAGER)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = CreateContractRequest.class),
                            examples = @ExampleObject(value = """
                                {
                                  "type": "LABOR",
                                  "startDate": "2024-09-01",
                                  "endDate": "2025-08-31",
                                  "status": "ACTIVE",
                                  "baseSalary": 18000000
                                }
                            """))
            )
    )
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/by-employee/{employeeId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Contract create(@PathVariable String employeeId, @Valid @RequestBody CreateContractRequest body) {
        Employee emp = employees.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        // tính version tiếp theo
        var all = contracts.findByEmployeeIdOrderByVersionDesc(employeeId);
        int nextVersion = all.stream().map(Contract::getVersion).max(Comparator.naturalOrder()).orElse(0) + 1;

        // Nếu tạo ACTIVE và có HĐ ACTIVE cũ -> expire HĐ cũ (policy đơn giản)
        if ("ACTIVE".equalsIgnoreCase(nz(body.status(), "ACTIVE"))) {
            all.stream()
                    .filter(c -> "ACTIVE".equalsIgnoreCase(nz(c.getStatus())))
                    .findFirst()
                    .ifPresent(c -> {
                        if (body.startDate() != null && c.getStartDate() != null && body.startDate().isAfter(c.getStartDate())) {
                            c.setStatus("EXPIRED");
                            contracts.save(c);
                        }
                    });
        }

        var c = Contract.builder()
                .employeeId(emp.getId())
                .type(body.type())
                .startDate(body.startDate())
                .endDate(body.endDate())
                .baseSalary(body.baseSalary())
                .status(nz(body.status(), "ACTIVE"))
                .version(nextVersion)
                .build();

        return contracts.save(c);
    }

    /* ===================== UPDATE ===================== */

    @Operation(
            summary = "Cập nhật hợp đồng (ADMIN/MANAGER)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = UpdateContractRequest.class),
                            examples = @ExampleObject(value = """
                                {
                                  "status": "EXPIRED",
                                  "baseSalary": 20000000
                                }
                            """))
            )
    )
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public Object update(@PathVariable String id, @Valid @RequestBody UpdateContractRequest body) {
        var c = contracts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found"));

        if (body.type() != null && !body.type().isBlank()) c.setType(body.type());
        if (body.startDate() != null) c.setStartDate(body.startDate());
        if (body.endDate() != null) c.setEndDate(body.endDate());
        if (body.status() != null && !body.status().isBlank()) c.setStatus(body.status());
        if (body.baseSalary() != null) c.setBaseSalary(body.baseSalary());

        contracts.save(c);
        return java.util.Map.of("message", "Contract updated");
    }

    /* ===================== Helpers ===================== */

    private void authorizeReadOwn(String employeeId, Authentication auth) {
        var me = (User) auth.getPrincipal();
        if (me.getRole() == User.Role.EMPLOYEE && !employeeId.equals(me.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private static String nz(String s, String def) { return (s == null || s.isBlank()) ? def : s; }
    private static String nz(String s) { return s == null ? "" : s; }
}
