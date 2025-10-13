// src/main/java/com/hrm/hrmapi/repo/EmployeeRepoCustom.java
package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeRepoCustom {
    Page<Employee> search(String q, String department, String status, Pageable pageable);
}
