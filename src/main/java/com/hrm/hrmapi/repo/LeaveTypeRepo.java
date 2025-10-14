package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.LeaveType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LeaveTypeRepo extends MongoRepository<LeaveType, String> {
    Optional<LeaveType> findByCodeIgnoreCase(String code);
}
