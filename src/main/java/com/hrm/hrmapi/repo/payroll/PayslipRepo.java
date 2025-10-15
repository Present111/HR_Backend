package com.hrm.hrmapi.repo.payroll;

import com.hrm.hrmapi.payroll.Payslip;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PayslipRepo extends MongoRepository<Payslip, String> {
    List<Payslip> findByCycleId(String cycleId);
    List<Payslip> findByEmployeeId(String employeeId);
}
