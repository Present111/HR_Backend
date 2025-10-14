package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.WorkSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkScheduleRepo extends MongoRepository<WorkSchedule, String> {
}
