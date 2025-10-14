package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.Doc;
import com.hrm.hrmapi.domain.User;
import com.hrm.hrmapi.repo.DocRepo;
import com.hrm.hrmapi.repo.EmployeeRepo;
import com.hrm.hrmapi.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Documents")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class DocumentsController {

    private final DocRepo docs;
    private final EmployeeRepo employees;
    private final FileStorageService storage;

    /* ===================== LIST ===================== */

    @Operation(summary = "Danh sách tài liệu của 1 nhân viên (lọc theo type). " +
            "Nếu không phải ADMIN thì ẩn tài liệu adminOnly=true.")
    @GetMapping("/employees/{id}/documents")
    public List<Doc> listByEmployee(
            @PathVariable String id,
            @Parameter(description = "Lọc theo loại tài liệu: CONTRACT | ID | CERT | OTHER ...")
            @RequestParam(required = false) String type,
            Authentication auth
    ) {
        var me = (User) auth.getPrincipal();

        // EMPLOYEE chỉ được xem chính mình
        if (me.getRole() == User.Role.EMPLOYEE && !Objects.equals(id, me.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        // đảm bảo employee tồn tại
        employees.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        List<Doc> found = StringUtils.hasText(type)
                ? docs.findByEmployeeIdAndTypeOrderByUploadedAtDesc(id, type)
                : docs.findByEmployeeIdOrderByUploadedAtDesc(id);

        // không phải admin -> ẩn adminOnly
        if (me.getRole() != User.Role.ADMIN) {
            found = found.stream().filter(d -> !d.isAdminOnly()).collect(Collectors.toList());
        }
        return found;
    }

    /* ===================== UPLOAD (MULTI) ===================== */

    @Operation(summary = "Upload nhiều tài liệu cho 1 nhân viên (MULTIPART). " +
            "ADMIN/MANAGER có thể upload cho bất kỳ nhân viên; EMPLOYEE chỉ upload cho chính mình. " +
            "`adminOnly` chỉ có tác dụng với ADMIN.")
    @PostMapping(value = "/employees/{id}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<Doc> upload(
            @PathVariable String id,
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(required = false, defaultValue = "OTHER") String type,
            @RequestParam(required = false, defaultValue = "false") boolean adminOnly,
            Authentication auth
    ) {
        var me = (User) auth.getPrincipal();

        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided");
        }

        // Quyền: employee chỉ tự upload cho mình
        boolean isAdmin = me.getRole() == User.Role.ADMIN;
        if (me.getRole() == User.Role.EMPLOYEE && !Objects.equals(id, me.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        // adminOnly chỉ cho ADMIN
        if (!isAdmin) adminOnly = false;

        // đảm bảo employee tồn tại
        employees.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        var saved = new ArrayList<Doc>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            // Lưu xuống /uploads/docs/{employeeId}/...
            String relativePath = storage.store(file, "docs/" + id);
            // Build URL public
            String url = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/")
                    .path(relativePath)
                    .toUriString();

            Doc d = Doc.builder()
                    .employeeId(id)
                    .type(StringUtils.hasText(type) ? type : "OTHER")
                    .name(Optional.ofNullable(file.getOriginalFilename()).orElse("document"))
                    .url(url)
                    .size(file.getSize())
                    .mime(Optional.ofNullable(file.getContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .uploadedAt(Instant.now())
                    .uploadedBy(me.getEmail())
                    .adminOnly(adminOnly)
                    .build();

            saved.add(docs.save(d));
        }

        if (saved.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All files are empty");
        }
        return saved;
    }

    /* ===================== DELETE ===================== */

    @Operation(summary = "Xoá 1 tài liệu theo id. " +
            "ADMIN xoá bất kỳ; MANAGER/EMPLOYEE chỉ xoá tài liệu không phải adminOnly " +
            "(và EMPLOYEE chỉ được xoá tài liệu của chính mình).")
    @DeleteMapping("/documents/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @NotBlank String docId, Authentication auth) {
        var me = (User) auth.getPrincipal();

        Doc d = docs.findById(docId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (me.getRole() != User.Role.ADMIN) {
            if (d.isAdminOnly()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
            }
            if (me.getRole() == User.Role.EMPLOYEE && !Objects.equals(d.getEmployeeId(), me.getEmployeeId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
            }
        }

        docs.deleteById(docId);
        // Nếu FileStorageService có hỗ trợ, xoá file vật lý khi URL trỏ vào /uploads
        try {
            storage.deleteIfLocal(d.getUrl());
        } catch (Exception ignored) {}
    }
}
