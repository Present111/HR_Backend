package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.LeaveQuota;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LeaveQuotaRepo extends MongoRepository<LeaveQuota, String> {
    Optional<LeaveQuota> findByEmployeeIdAndYear(String employeeId, Integer year);
}
