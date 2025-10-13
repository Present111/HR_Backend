package com.hrm.hrmapi.repo;
import com.hrm.hrmapi.domain.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;

public interface ContractRepo extends MongoRepository<Contract, String> {
    List<Contract> findByEmployeeIdOrderByVersionDesc(String employeeId);
}