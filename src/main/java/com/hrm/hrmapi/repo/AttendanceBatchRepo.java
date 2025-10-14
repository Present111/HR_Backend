package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.AttendanceBatch;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AttendanceBatchRepo extends MongoRepository<AttendanceBatch, String> { }
