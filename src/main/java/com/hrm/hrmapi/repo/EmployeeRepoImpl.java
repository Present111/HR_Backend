// src/main/java/com/hrm/hrmapi/repo/EmployeeRepoImpl.java
package com.hrm.hrmapi.repo;

import com.hrm.hrmapi.domain.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

@Repository
public class EmployeeRepoImpl implements EmployeeRepoCustom {
    @Autowired
    private MongoTemplate mongo;

    @Override
    public Page<Employee> search(String q, String department, String status, Pageable pageable) {
        List<Criteria> and = new ArrayList<>();

        if (StringUtils.hasText(q)) {
            Pattern regex = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
            and.add(new Criteria().orOperator(
                    Criteria.where("code").regex(regex),
                    Criteria.where("fullName").regex(regex),
                    Criteria.where("phone").regex(regex),
                    Criteria.where("position").regex(regex),
                    Criteria.where("department").regex(regex)
            ));
        }
        if (StringUtils.hasText(department)) {
            and.add(Criteria.where("department").is(department));
        }
        if (StringUtils.hasText(status)) {
            and.add(Criteria.where("status").is(status));
        }

        Criteria criteria = and.isEmpty() ? new Criteria() : new Criteria().andOperator(and);
        Query query = new Query(criteria).with(pageable);

        List<Employee> items = mongo.find(query, Employee.class);
        long total = mongo.count(Query.of(query).limit(-1).skip(-1), Employee.class);
        return new PageImpl<>(items, pageable, total);
    }
}
