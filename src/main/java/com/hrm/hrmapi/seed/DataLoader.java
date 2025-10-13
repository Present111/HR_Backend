// seed/DataLoader.java
package com.hrm.hrmapi.seed;
import com.hrm.hrmapi.domain.*; import com.hrm.hrmapi.repo.*;
import lombok.RequiredArgsConstructor; import org.springframework.boot.CommandLineRunner; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate; import java.util.List;

@Configuration @RequiredArgsConstructor
public class DataLoader {
    private final PasswordEncoder pe;

    @Bean CommandLineRunner load(UserRepo users, EmployeeRepo employees, ContractRepo contracts, DocRepo docs, DepartmentRepo deps){
        return args -> {
            if (users.count() > 0) return;

            var e1 = employees.save(Employee.builder()
                    .code("NV-2025-0001").fullName("Nguyễn An").department("HR").position("HR Executive")
                    .status("ACTIVE").joinDate(LocalDate.of(2024,3,1))
                    .phone("0901234567").address("Q1, HCM").build());

            var e2 = employees.save(Employee.builder()
                    .code("NV-2024-0102").fullName("Trần Bình").department("IT").position("Backend Dev")
                    .status("ACTIVE").joinDate(LocalDate.of(2023,10,15)).build());

            deps.saveAll(List.of(
                    Department.builder().name("HR").manager("Mai").build(),
                    Department.builder().name("IT").manager("Quân").build(),
                    Department.builder().name("Sales").manager("Hạnh").build()
            ));

            contracts.saveAll(List.of(
                    Contract.builder().employeeId(e1.getId()).type("INDEFINITE").startDate(LocalDate.of(2024,3,1))
                            .baseSalary(new BigDecimal("1200")).status("ACTIVE").version(2).build(),
                    Contract.builder().employeeId(e1.getId()).type("FIXED-12M").startDate(LocalDate.of(2023,3,1))
                            .endDate(LocalDate.of(2024,2,29)).baseSalary(new BigDecimal("1000")).status("EXPIRED").version(1).build(),
                    Contract.builder().employeeId(e2.getId()).type("FIXED-12M").startDate(LocalDate.of(2023,10,15))
                            .baseSalary(new BigDecimal("1400")).status("ACTIVE").version(1).build()
            ));

            docs.saveAll(List.of(
                    Doc.builder()
                            .employeeId(e1.getId())
                            .type("CCCD")  // Đổi từ .kind() sang .type()
                            .name("CCCD_An.pdf")
                            .url("#")
                            .uploadedAt(Instant.now())
                            .build(),
                    Doc.builder()
                            .employeeId(e1.getId())
                            .type("CONTRACT")  // Đổi từ .kind() sang .type()
                            .name("Contract_2024.pdf")
                            .url("#")
                            .uploadedAt(Instant.now())
                            .build(),
                    Doc.builder()
                            .employeeId(e1.getId())
                            .type("CONTRACT")  // Đổi từ .kind() sang .type()
                            .name("Contract_2025.pdf")
                            .url("#")
                            .uploadedAt(Instant.now())
                            .build()
            ));
            users.saveAll(List.of(
                    User.builder().email("admin@hrm.local").fullName("Admin").role(User.Role.ADMIN).passwordHash(pe.encode("admin")).build(),
                    User.builder().email("manager@hrm.local").fullName("Manager Mai").role(User.Role.MANAGER).passwordHash(pe.encode("manager")).build(),
                    User.builder().email("emp@hrm.local").fullName("Employee Em").role(User.Role.EMPLOYEE).passwordHash(pe.encode("employee"))
                            .employeeId(e1.getId()).build()
            ));
        };
    }
}
