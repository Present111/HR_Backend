package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.Doc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DocRepo extends MongoRepository<Doc, String> {
    List<Doc> findByEmployeeIdOrderByUploadedAtDesc(String employeeId);
    List<Doc> findByEmployeeIdAndTypeOrderByUploadedAtDesc(String employeeId, String type);
}
