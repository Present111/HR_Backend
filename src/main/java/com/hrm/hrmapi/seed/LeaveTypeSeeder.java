// src/main/java/com/hrm/hrmapi/seed/LeaveTypeSeeder.java
package com.hrm.hrmapi.seed;

import com.hrm.hrmapi.domain.LeaveType;
import com.hrm.hrmapi.repo.LeaveTypeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaveTypeSeeder implements CommandLineRunner {
    private final LeaveTypeRepo typeRepo;

    @Override public void run(String... args) {
        upsert("AL", "Annual Leave", true);
        upsert("SL", "Sick Leave", true);
        upsert("UL", "Unpaid Leave", false);
    }

    private void upsert(String code, String name, boolean deductQuota) {
        typeRepo.findByCodeIgnoreCase(code).orElseGet(() ->
                typeRepo.save(LeaveType.builder()
                        .code(code).name(name).deductQuota(deductQuota).build()));
    }
}
