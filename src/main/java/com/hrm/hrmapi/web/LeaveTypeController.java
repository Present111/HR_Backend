// web/LeaveTypeController.java
package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.LeaveType;
import com.hrm.hrmapi.repo.LeaveTypeRepo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leave/types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeRepo repo;

    @Operation(summary = "List all leave types")
    @GetMapping
    public List<LeaveType> list() {
        return repo.findAll();
    }

    @Operation(summary = "Create")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveType create(@Valid @RequestBody LeaveType body) {
        return repo.save(body);
    }

    @Operation(summary = "Update")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public LeaveType update(@PathVariable String id, @Valid @RequestBody LeaveType body) {
        body.setId(id);
        return repo.save(body);
    }

    @Operation(summary = "Delete")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        repo.deleteById(id);
    }
}
