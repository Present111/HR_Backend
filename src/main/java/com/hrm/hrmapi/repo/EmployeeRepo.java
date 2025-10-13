package com.hrm.hrmapi.repo;
import com.hrm.hrmapi.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;

public interface EmployeeRepo extends MongoRepository<Employee, String> {
    List<Employee> findByDepartment(String department);
}
