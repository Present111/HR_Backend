package com.hrm.hrmapi.repo;
import com.hrm.hrmapi.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;

public interface UserRepo extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmployeeId(String employeeId);
    boolean existsByEmail(String email);
    boolean existsByEmployeeId(String employeeId);
}