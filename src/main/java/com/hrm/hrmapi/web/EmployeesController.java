// web/EmployeesController.java
package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.Employee;
import com.hrm.hrmapi.domain.User;
import com.hrm.hrmapi.repo.ContractRepo;
import com.hrm.hrmapi.repo.EmployeeRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Employees")
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeesController {

    private final EmployeeRepo employees;
    private final ContractRepo contracts;

    /* ===================== LIST (filter + pagination + sort) ===================== */

    @Operation(summary = "Danh sách nhân viên (filter + pagination)")
    @GetMapping
    public Map<String, Object> list(
            @Parameter(description = "Tìm theo code/fullName/phone/position/department (contains, ignore case)")
            @RequestParam(required = false) String search,
            @Parameter(description = "Lọc theo phòng ban (exact, ignore case)")
            @RequestParam(required = false) String department,
            @Parameter(description = "Lọc theo trạng thái (ACTIVE/INACTIVE, exact, ignore case)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Trang, bắt đầu từ 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "VD: fullName,asc | joinDate,desc") @RequestParam(defaultValue = "joinDate,desc") String sort
    ) {
        // Lấy toàn bộ & lọc trong bộ nhớ – Sprint 1 cho nhanh
        List<Employee> all = employees.findAll();

        // filter
        String q = StringUtils.hasText(search) ? search.trim().toLowerCase() : null;
        String dept = StringUtils.hasText(department) ? department.trim().toLowerCase() : null;
        String st = StringUtils.hasText(status) ? status.trim().toLowerCase() : null;

        List<Employee> filtered = all.stream()
                .filter(e -> {
                    if (q == null) return true;
                    return containsIgnoreCase(e.getCode(), q)
                            || containsIgnoreCase(e.getFullName(), q)
                            || containsIgnoreCase(e.getPhone(), q)
                            || containsIgnoreCase(e.getPosition(), q)
                            || containsIgnoreCase(e.getDepartment(), q);
                })
                .filter(e -> dept == null || equalsIgnoreCase(e.getDepartment(), dept))
                .filter(e -> st == null || equalsIgnoreCase(e.getStatus(), st))
                .collect(Collectors.toList());

        // sort
        Comparator<Employee> cmp = buildComparator(sort);
        if (cmp != null) filtered.sort(cmp);

        // pagination
        long total = filtered.size();
        int from = Math.max(0, page * size);
        int to = Math.min(filtered.size(), from + size);
        List<Employee> items = from >= to ? Collections.emptyList() : filtered.subList(from, to);

        return Map.of(
                "items", items,
                "page", page,
                "size", size,
                "total", total
        );
    }

    /* ===================== EXPORT CSV ===================== */

    @Operation(summary = "Export danh sách nhân viên (CSV) theo filter hiện tại")
    @GetMapping("/export")
    public void exportCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            HttpServletResponse resp
    ) throws Exception {
        // Tái sử dụng logic filter như trên
        List<Employee> all = employees.findAll();

        String q = StringUtils.hasText(search) ? search.trim().toLowerCase() : null;
        String dept = StringUtils.hasText(department) ? department.trim().toLowerCase() : null;
        String st = StringUtils.hasText(status) ? status.trim().toLowerCase() : null;

        List<Employee> filtered = all.stream()
                .filter(e -> {
                    if (q == null) return true;
                    return containsIgnoreCase(e.getCode(), q)
                            || containsIgnoreCase(e.getFullName(), q)
                            || containsIgnoreCase(e.getPhone(), q)
                            || containsIgnoreCase(e.getPosition(), q)
                            || containsIgnoreCase(e.getDepartment(), q);
                })
                .filter(e -> dept == null || equalsIgnoreCase(e.getDepartment(), dept))
                .filter(e -> st == null || equalsIgnoreCase(e.getStatus(), st))
                .collect(Collectors.toList());

        String filename = "employees.csv";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        resp.setStatus(HttpStatus.OK.value());
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);

        try (PrintWriter w = resp.getWriter()) {
            w.println("Code,Full Name,Department,Position,Status,Join Date,Phone,Address");
            for (Employee e : filtered) {
                w.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        nz(e.getCode()), nz(e.getFullName()), nz(e.getDepartment()),
                        nz(e.getPosition()), nz(e.getStatus()),
                        nz(e.getJoinDate()), nz(e.getPhone()), nz(e.getAddress()));
            }
        }
    }

    /* ===================== DETAIL / CONTRACTS ===================== */

    @Operation(summary = "Chi tiết nhân viên; EMPLOYEE chỉ xem hồ sơ của chính mình")
    @GetMapping("/{id}")
    public Employee one(@PathVariable String id, Authentication auth){
        var me = (User) auth.getPrincipal();
        if (me.getRole() == User.Role.EMPLOYEE && !id.equals(me.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return employees.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @Operation(summary = "Danh sách hợp đồng theo nhân viên")
    @GetMapping("/{id}/contracts")
    public Object empContracts(@PathVariable String id){
        return contracts.findByEmployeeIdOrderByVersionDesc(id);
    }

    /* ===================== CREATE ===================== */

    public record CreateEmployeeRequest(
            @Schema(example = "NV-2025-0100") @NotBlank @Size(max = 50) String code,
            @Schema(example = "Phạm Hoài Nam") @NotBlank @Size(max = 120) String fullName,
            @Schema(example = "IT") @NotBlank @Size(max = 60) String department,
            @Schema(example = "Frontend Dev") @NotBlank @Size(max = 80) String position,
            @Schema(example = "ACTIVE") @NotBlank String status,
            @Schema(example = "2024-08-20") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDate,
            @Schema(example = "0902345678") String phone,
            @Schema(example = "Q.7, HCM") String address
    ) {}

    @Operation(
            summary = "Tạo nhân viên (chỉ ADMIN/MANAGER)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            schema = @Schema(implementation = CreateEmployeeRequest.class),
                            examples = @ExampleObject(value = """
                {
                  "code": "NV-2025-0100",
                  "fullName": "Phạm Hoài Nam",
                  "department": "IT",
                  "position": "Frontend Dev",
                  "status": "ACTIVE",
                  "joinDate": "2024-08-20",
                  "phone": "0902345678",
                  "address": "Q.7, HCM"
                }
              """)
                    )
            )
    )
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Employee create(@Valid @RequestBody CreateEmployeeRequest req) {
        // Kiểm tra trùng code
        employees.findAll().stream()
                .filter(e -> e.getCode() != null && e.getCode().equalsIgnoreCase(req.code()))
                .findAny()
                .ifPresent(e -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee code already exists"); });

        var emp = Employee.builder()
                .code(req.code())
                .fullName(req.fullName())
                .department(req.department())
                .position(req.position())
                .status(req.status())
                .joinDate(req.joinDate() != null ? req.joinDate() : LocalDate.now())
                .phone(req.phone())
                .address(req.address())
                .build();

        return employees.save(emp);
    }

    /* ===================== UPDATE INFO (CONTACT) & JOB ===================== */

    // DTO cho Info tab
    public record UpdateContactReq(
            String fullName,
            String phone,
            String address,
            Emergency emergency
    ){
        public record Emergency(String name, String phone, String relation){}
    }

    // DTO cho Job tab
    public record UpdateJobReq(
            String department,
            String position,
            String status,  // ACTIVE/INACTIVE...
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinDate,
            String grade,        // optional nếu sau này có field
            String contractType  // optional nếu sau này có field
    ) {}

    @Operation(summary = "Cập nhật thông tin liên hệ + liên hệ khẩn (Info tab)")
    @PutMapping("/{id}/contact")
    public Map<String,Object> updateContact(
            @PathVariable String id,
            @RequestBody @Valid UpdateContactReq req,
            Authentication auth
    ){
        ensureCanEdit(auth, id);
        var emp = employees.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (req.fullName()!=null && !req.fullName().isBlank()) emp.setFullName(req.fullName());
        if (req.phone()!=null)   emp.setPhone(req.phone());
        if (req.address()!=null) emp.setAddress(req.address());
        if (req.emergency()!=null){
            var e = new Employee.Emergency();
            e.setName(req.emergency().name());
            e.setPhone(req.emergency().phone());
            e.setRelation(req.emergency().relation());
            emp.setEmergencyContact(e);
        }
        employees.save(emp);
        return Map.of("message","updated");
    }

    @Operation(summary = "Cập nhật thông tin công việc (Job tab)")
    @PutMapping("/{id}/job")
    public Map<String,Object> updateJob(
            @PathVariable String id,
            @RequestBody @Valid UpdateJobReq req,
            Authentication auth
    ){
        ensureCanEdit(auth, id);
        var emp = employees.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (req.department()!=null) emp.setDepartment(req.department());
        if (req.position()!=null)   emp.setPosition(req.position());
        if (req.status()!=null)     emp.setStatus(req.status());
        if (req.joinDate()!=null)   emp.setJoinDate(req.joinDate());
        // grade / contractType: set nếu entity có

        employees.save(emp);
        return Map.of("message","updated");
    }

    /* ===================== Helpers ===================== */

    private void ensureCanEdit(Authentication auth, String employeeId){
        var me = (User) auth.getPrincipal();
        if (me.getRole() == User.Role.EMPLOYEE && !employeeId.equals(me.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private static boolean containsIgnoreCase(String s, String needleLower) {
        return s != null && s.toLowerCase().contains(needleLower);
    }

    private static boolean equalsIgnoreCase(String s, String tLower) {
        return s != null && s.equalsIgnoreCase(tLower);
    }

    private static String nz(Object v) { return v == null ? "" : v.toString(); }

    private Comparator<Employee> buildComparator(String sort) {
        if (!StringUtils.hasText(sort)) return null;
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1]);

        Comparator<Employee> c;
        switch (field) {
            case "code" -> c = Comparator.comparing(Employee::getCode, Comparator.nullsLast(String::compareToIgnoreCase));
            case "fullName" -> c = Comparator.comparing(Employee::getFullName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "department" -> c = Comparator.comparing(Employee::getDepartment, Comparator.nullsLast(String::compareToIgnoreCase));
            case "position" -> c = Comparator.comparing(Employee::getPosition, Comparator.nullsLast(String::compareToIgnoreCase));
            case "status" -> c = Comparator.comparing(Employee::getStatus, Comparator.nullsLast(String::compareToIgnoreCase));
            case "joinDate" -> c = Comparator.comparing(Employee::getJoinDate, Comparator.nullsLast(LocalDate::compareTo));
            default -> c = Comparator.comparing(Employee::getFullName, Comparator.nullsLast(String::compareToIgnoreCase));
        }
        return desc ? c.reversed() : c;
    }
}
