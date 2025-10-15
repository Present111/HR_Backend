// src/main/java/com/hrm/hrmapi/repo/HolidayRepo.java
package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.Holiday;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepo extends MongoRepository<Holiday, String> {
    List<Holiday> findByDateBetween(LocalDate start, LocalDate end);
}
