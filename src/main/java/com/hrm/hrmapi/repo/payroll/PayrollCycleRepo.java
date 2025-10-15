package com.hrm.hrmapi.repo.payroll;

import com.hrm.hrmapi.payroll.PayrollCycle;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PayrollCycleRepo extends MongoRepository<PayrollCycle, String> {}
