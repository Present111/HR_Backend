package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.AttendanceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepo extends MongoRepository<AttendanceRecord, String> {
    Optional<AttendanceRecord> findByEmployeeIdAndDate(String employeeId, LocalDate date);
    List<AttendanceRecord> findByEmployeeIdAndDateBetween(String employeeId, LocalDate from, LocalDate to);
    List<AttendanceRecord> findByDateBetween(LocalDate from, LocalDate to);
}
