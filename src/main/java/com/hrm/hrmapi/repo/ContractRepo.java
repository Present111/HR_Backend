// src/main/java/com/hrm/hrmapi/repo/ContractRepo.java
package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.Contract;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContractRepo extends MongoRepository<Contract, String> {

    // Trả về tất cả HĐ active tại thời điểm 'at'
    @Query("{ 'employeeId': ?0, 'status': 'ACTIVE', " +
            "  'startDate': { $lte: ?1 }, " +
            "  $or: [ {'endDate': null}, {'endDate': { $gte: ?1 }} ] }")
    List<Contract> findActiveAtAll(String employeeId, LocalDate at);

    default Optional<Contract> findActiveByEmployee(String employeeId, LocalDate at) {
        return findActiveAtAll(employeeId, at).stream()
                .max(java.util.Comparator.comparingInt(Contract::getVersion));
        // hoặc so sánh theo startDate nếu bạn thích
    }

    List<Contract> findByEmployeeIdOrderByVersionDesc(String employeeId);
}

