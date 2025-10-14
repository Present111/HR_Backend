package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.LeaveRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepo extends MongoRepository<LeaveRequest, String> {
    List<LeaveRequest> findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String employeeId, LocalDate to, LocalDate from);
    List<LeaveRequest> findByStatus(String status);
    List<LeaveRequest> findByStartDateBetween(LocalDate from, LocalDate to);
}
