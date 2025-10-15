package com.hrm.hrmapi.repo;
import com.hrm.hrmapi.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.*;

public interface EmployeeRepo extends MongoRepository<Employee, String>, EmployeeRepoCustom {
    List<Employee> findByDepartment(String department);
    // Tuỳ model bạn định nghĩa trạng thái. Nếu có field status = "ACTIVE" thì dùng cách này:
    @Query("{ 'status': 'ACTIVE' }")
    List<Employee> findAllActive();

    // Hoặc active theo thời gian (có contract hiệu lực) thì dùng findAll() và lọc ở Service
    default List<Employee> findAllActive(LocalDate refDate) {
        return findAll(); // để đơn giản, PayrollService sẽ tự lọc theo contract
    }
}
