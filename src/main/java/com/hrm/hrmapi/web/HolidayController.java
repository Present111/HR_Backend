package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.Holiday;
import com.hrm.hrmapi.repo.HolidayRepo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayRepo repo;

    @Operation(summary = "List holidays by year")
    @GetMapping
    public List<Holiday> list(@RequestParam(required = false) Integer year) {
        int y = (year != null) ? year : Year.now().getValue();
        LocalDate from = LocalDate.of(y, 1, 1);
        LocalDate to = LocalDate.of(y, 12, 31);
        return repo.findByDateBetween(from, to);
    }

    @Operation(summary = "Create holiday")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Holiday create(@Valid @RequestBody Holiday body) {
        return repo.save(body);
    }

    @Operation(summary = "Update holiday")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public Holiday update(@PathVariable String id, @Valid @RequestBody Holiday body) {
        body.setId(id);
        return repo.save(body);
    }

    @Operation(summary = "Delete holiday")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        repo.deleteById(id);
    }
}
