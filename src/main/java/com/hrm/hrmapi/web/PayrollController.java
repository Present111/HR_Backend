// src/main/java/com/hrm/hrmapi/web/PayrollController.java
package com.hrm.hrmapi.web;

import com.hrm.hrmapi.payroll.PayrollCycle;
import com.hrm.hrmapi.payroll.Payslip;
import com.hrm.hrmapi.repo.payroll.PayrollCycleRepo;
import com.hrm.hrmapi.repo.payroll.PayslipRepo;
import com.hrm.hrmapi.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/payroll", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Payroll", description = "Quản lý kỳ lương, tính lương và phiếu lương")
@SecurityRequirement(name = "bearerAuth") // Swagger sẽ hiện nút Authorize (JWT)
public class PayrollController {

    private final PayrollCycleRepo cycleRepo;
    private final PayslipRepo payslipRepo;
    private final PayrollService payrollService;

    // ===== Cycles =====

    @Operation(
            summary = "Tạo kỳ lương",
            description = "Tạo một kỳ lương mới. Nếu không truyền id thì sẽ lấy theo định dạng yyyy-MM của ngày bắt đầu.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tạo thành công",
                            content = @Content(schema = @Schema(implementation = PayrollCycle.class))),
                    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
            }
    )
    @PostMapping(value = "/cycles", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PayrollCycle createCycle(@Valid @RequestBody CreateCycleReq req) {
        String id = (req.getId() != null && !req.getId().isBlank())
                ? req.getId()
                : req.getStart().getYear() + "-" + String.format("%02d", req.getStart().getMonthValue());
        return payrollService.createCycle(id, req.getStart(), req.getEnd(), req.getCurrency(), req.getName());
    }

    @Operation(summary = "Danh sách kỳ lương")
    @GetMapping("/cycles")
    public List<PayrollCycle> listCycles() {
        return cycleRepo.findAll();
    }

    // ===== Calculation =====

    @Operation(
            summary = "Tính lương",
            description = "Tính lương cho 1 nhân viên hoặc toàn bộ nhân viên trong kỳ. " +
                    "Nếu truyền employeeId thì chỉ tính cho 1 người.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Đã tính xong"),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy kỳ lương hoặc nhân viên")
            }
    )
    @PostMapping("/cycles/{cycleId}/calculate")
    public ResponseEntity<?> calculate(
            @Parameter(description = "ID kỳ lương", example = "2025-11")
            @PathVariable String cycleId,
            @Parameter(description = "ID nhân viên, tùy chọn")
            @RequestParam(required = false) String employeeId
    ) {
        if (employeeId != null && !employeeId.isBlank()) {
            return ResponseEntity.ok(payrollService.calculateForEmployee(cycleId, employeeId));
        }
        return ResponseEntity.ok(payrollService.calculateForAll(cycleId));
    }

    // ===== Payslips =====

    @Operation(summary = "Danh sách payslip trong kỳ")
    @GetMapping("/cycles/{cycleId}/payslips")
    public List<Payslip> listPayslips(
            @Parameter(description = "ID kỳ lương", example = "2025-11")
            @PathVariable String cycleId
    ) {
        return payslipRepo.findByCycleId(cycleId);
    }

    @Operation(summary = "Chi tiết một payslip")
    @GetMapping("/payslips/{id}")
    public Payslip getPayslip(@Parameter(description = "ID payslip") @PathVariable String id) {
        return payslipRepo.findById(id).orElseThrow();
    }

    // ===== DTO =====
    @Data
    public static class CreateCycleReq {
        @Schema(description = "ID kỳ lương, nếu bỏ trống hệ thống sẽ tạo theo yyyy-MM", example = "2025-11")
        private String id;

        @Schema(description = "Tên hiển thị kỳ lương", example = "Tháng 11/2025")
        private String name;

        @Schema(description = "Tiền tệ", example = "VND", defaultValue = "VND")
        private String currency;

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(description = "Ngày bắt đầu", example = "2025-11-01", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDate start;

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(description = "Ngày kết thúc", example = "2025-11-30", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDate end;
    }
}
