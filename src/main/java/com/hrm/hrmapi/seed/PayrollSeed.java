package com.hrm.hrmapi.seed;

import com.hrm.hrmapi.payroll.PayrollComponent;
import com.hrm.hrmapi.repo.payroll.PayrollComponentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import static com.hrm.hrmapi.payroll.PayrollEnums.CalcType;
import static com.hrm.hrmapi.payroll.PayrollEnums.ComponentKind;

@Configuration
@RequiredArgsConstructor
public class PayrollSeed implements CommandLineRunner {

    private final PayrollComponentRepo components;

    @Override
    public void run(String... args) {
        seed("BASE_SALARY", "Lương cơ bản",
                ComponentKind.EARNING, CalcType.FORMULA,
                "base_salary_prorated", 10);

        seed("OT_WEEKDAY", "OT ngày thường",
                ComponentKind.EARNING, CalcType.FORMULA,
                "ot_minutes_weekday / 60 * base_hourly * 1.5", 20);

        seed("OT_WEEKEND", "OT cuối tuần",
                ComponentKind.EARNING, CalcType.FORMULA,
                "ot_minutes_weekend / 60 * base_hourly * 2.0", 21);

        seed("OT_HOLIDAY", "OT ngày lễ",
                ComponentKind.EARNING, CalcType.FORMULA,
                "ot_minutes_holiday / 60 * base_hourly * 3.0", 22);

        seed("ALLOWANCE_FIXED", "Phụ cấp cố định",
                ComponentKind.EARNING, CalcType.FIXED,
                null, 30);

        seed("BONUS", "Thưởng",
                ComponentKind.EARNING, CalcType.FIXED,
                null, 40);
    }

    private void seed(
            String id, String label,
            com.hrm.hrmapi.payroll.PayrollEnums.ComponentKind kind,
            com.hrm.hrmapi.payroll.PayrollEnums.CalcType type,
            String expr, int priority
    ) {
        if (components.existsById(id)) return;
        components.save(PayrollComponent.builder()
                .id(id)
                .label(label)
                .kind(kind)
                .calcType(type)
                .expr(expr)
                .priority(priority)
                .active(true)
                .build());
    }
}
