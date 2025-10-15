package com.hrm.hrmapi.repo.payroll;

import com.hrm.hrmapi.payroll.PayrollComponent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PayrollComponentRepo extends MongoRepository<PayrollComponent, String> {
    List<PayrollComponent> findAllByActiveTrueOrderByPriorityAsc();
    boolean existsById(String id);
}
