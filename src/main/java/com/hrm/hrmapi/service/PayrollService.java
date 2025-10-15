package com.hrm.hrmapi.service;

import com.hrm.hrmapi.domain.Employee;
import com.hrm.hrmapi.payroll.*;
import com.hrm.hrmapi.repo.ContractRepo;
import com.hrm.hrmapi.repo.EmployeeRepo;
import com.hrm.hrmapi.repo.payroll.PayrollComponentRepo;
import com.hrm.hrmapi.repo.payroll.PayrollCycleRepo;
import com.hrm.hrmapi.repo.payroll.PayslipRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.hrm.hrmapi.payroll.PayrollEnums.ComponentKind;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollCycleRepo cycleRepo;
    private final PayrollComponentRepo componentRepo;
    private final PayslipRepo payslipRepo;
    private final ContractRepo contractRepo;
    private final EmployeeRepo employeeRepo;          // đã có trong project
    private final AttendanceSummaryService summaryService;

    public PayrollCycle createCycle(String id, LocalDate start, LocalDate end, String currency, String name) {
        var cycle = PayrollCycle.builder()
                .id(id)
                .name(name != null ? name : id)
                .startDate(start)
                .endDate(end)
                .currency(currency == null ? "VND" : currency)
                .status(PayrollEnums.CycleStatus.DRAFT)
                .build();
        return cycleRepo.save(cycle);
    }

    public Payslip calculateForEmployee(String cycleId, String employeeId) {
        var cycle = cycleRepo.findById(cycleId).orElseThrow();
        var emp = employeeRepo.findById(employeeId).orElseThrow();

        var sum = summaryService.summarize(employeeId, cycle.getStartDate(), cycle.getEndDate());
        var items = new ArrayList<Payslip.Item>();

        // 1) Lương cơ bản theo ngày công
        BigDecimal baseProrated = sum.getBaseSalary()
                .multiply(BigDecimal.valueOf(sum.getWorkingDaysPaid()))
                .divide(BigDecimal.valueOf(sum.getWorkingDaysInCycle()), 0, RoundingMode.HALF_UP);
        items.add(new Payslip.Item("BASE_SALARY", "Lương cơ bản", "EARNING", baseProrated));

        // 2) OT
        BigDecimal otWeekday = BigDecimal.valueOf(sum.getOtMinutesWeekday())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .multiply(sum.getBaseHourly()).multiply(BigDecimal.valueOf(1.5));
        items.add(new Payslip.Item("OT_WEEKDAY", "OT ngày thường", "EARNING", otWeekday));

        BigDecimal otWeekend = BigDecimal.valueOf(sum.getOtMinutesWeekend())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .multiply(sum.getBaseHourly()).multiply(BigDecimal.valueOf(2.0));
        items.add(new Payslip.Item("OT_WEEKEND", "OT cuối tuần", "EARNING", otWeekend));

        BigDecimal otHoliday = BigDecimal.valueOf(sum.getOtMinutesHoliday())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .multiply(sum.getBaseHourly()).multiply(BigDecimal.valueOf(3.0));
        items.add(new Payslip.Item("OT_HOLIDAY", "OT ngày lễ", "EARNING", otHoliday));

        // 3) Phụ cấp cố định + Thưởng (nếu có chính sách riêng, ta cộng thêm ở bước sau)
        // Tạm để 0, sẽ có API thêm phụ cấp/bonus vào payslip item.
        items.add(new Payslip.Item("ALLOWANCE_FIXED", "Phụ cấp cố định", "EARNING", BigDecimal.ZERO));
        items.add(new Payslip.Item("BONUS", "Thưởng", "EARNING", BigDecimal.ZERO));

        // 4) Tổng hợp
        BigDecimal gross = sumByKind(items, "EARNING");
        BigDecimal deductions = sumByKind(items, "DEDUCTION");
        BigDecimal net = gross.subtract(deductions);

        var payslip = Payslip.builder()
                .cycleId(cycleId)
                .employeeId(emp.getId())
                .items(items)
                .gross(gross)
                .deductions(deductions)
                .net(net)
                .status("CALCULATED")
                .generatedAt(cycle.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant())
                .build();

        // nếu đã có payslip của kỳ, ghi đè
        payslipRepo.findByCycleId(cycleId).stream()
                .filter(p -> p.getEmployeeId().equals(emp.getId()))
                .findFirst()
                .ifPresent(p -> payslip.setId(p.getId()));

        return payslipRepo.save(payslip);
    }

    public List<Payslip> calculateForAll(String cycleId) {
        PayrollCycle cycle = cycleRepo.findById(cycleId).orElseThrow();

        // Nếu chưa có hàm findAllActive(endDate) thì tạm thời lấy tất cả nhân viên ACTIVE
        // và khi vào từng nhân viên sẽ kiểm tra HĐ active bằng contractRepo:
        List<Employee> employees = employeeRepo.findAll();  // hoặc findByStatus("ACTIVE")

        List<Payslip> result = new ArrayList<>();
        for (var e : employees) {
            // Bỏ qua nhân viên không có HĐ active tại ngày endDate
            if (contractRepo.findActiveByEmployee(e.getId(), cycle.getEndDate()).isEmpty()) continue;
            result.add(calculateForEmployee(cycleId, e.getId()));
        }
        return result;
    }

    private BigDecimal sumByKind(List<Payslip.Item> items, String kind) {
        return items.stream()
                .filter(i -> kind.equals(i.getKind()))
                .map(Payslip.Item::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
