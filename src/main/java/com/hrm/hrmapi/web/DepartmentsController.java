package com.hrm.hrmapi.web;

import com.hrm.hrmapi.repo.DepartmentRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Departments")
@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentsController {
    private final DepartmentRepo repo;

    @Operation(summary = "Danh sách phòng ban")
    @GetMapping
    public Object all() { return repo.findAll(); }
}
