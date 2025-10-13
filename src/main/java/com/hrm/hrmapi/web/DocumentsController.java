package com.hrm.hrmapi.web;

import com.hrm.hrmapi.domain.Doc;
import com.hrm.hrmapi.repo.DocRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/employees/{empId}/documents")
public class DocumentsController {

    private final DocRepo docs;

    public record DocReq(
            @Schema(example = "CONTRACT") String type,
            @Schema(example = "HĐLĐ 2025.pdf") String name,
            @Schema(example = "https://files.example/hdld-2025.pdf") String url,
            @Schema(example = "application/pdf") String mime,
            @Schema(example = "false") Boolean adminOnly,
            @Schema(example = "102400") Long size
    ) {}

    public record DocRes(
            String id, String employeeId, String type, String name, String url,
            String mime, Long size, Instant uploadedAt, String uploadedBy, Boolean adminOnly
    ) {}

    private static DocRes toRes(Doc d) {
        return new DocRes(
                d.getId(), d.getEmployeeId(), d.getType(), d.getName(), d.getUrl(),
                d.getMime(), d.getSize(), d.getUploadedAt(), d.getUploadedBy(), d.isAdminOnly()
        );
    }

    @Operation(summary = "Danh sách tài liệu của 1 employee", description = "Tham số type là danh sách, ví dụ: type=CONTRACT,ID")
    @GetMapping
    public List<DocRes> list(@PathVariable String empId,
                             @RequestParam(required = false) String type,
                             Authentication auth) {

        // ví dụ: ẩn tài liệu adminOnly nếu không phải ADMIN
        var isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        List<Doc> items;
        if (type == null || type.isBlank() || type.equalsIgnoreCase("ALL")) {
            items = docs.findByEmployeeId(empId);
        } else {
            var types = Arrays.stream(type.split(",")).map(String::trim).toList();
            items = docs.findByEmployeeIdAndTypeIn(empId, types);
        }

        return items.stream()
                .filter(d -> isAdmin || !Boolean.TRUE.equals(d.isAdminOnly()))
                .map(DocumentsController::toRes)
                .toList();
    }

    @Operation(
            summary = "Tạo metadata tài liệu (upload đơn giản)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = DocReq.class),
                            examples = @ExampleObject(value = """
                {"type":"ID","name":"CMND.pdf","url":"https://files/emp01/cmnd.pdf","mime":"application/pdf","size":234567}
              """)
                    )
            ),
            responses = @ApiResponse(responseCode = "201", description = "Created")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocRes create(@PathVariable String empId, @RequestBody DocReq body, Authentication auth) {
        if (body.url() == null || body.url().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing file url");
        }
        var uEmail = auth != null ? auth.getName() : "system";
        var d = Doc.builder()
                .employeeId(empId)
                .type(body.type() == null || body.type().isBlank() ? "OTHER" : body.type().trim())
                .name(body.name() == null || body.name().isBlank() ? "Untitled" : body.name().trim())
                .url(body.url().trim())
                .mime(body.mime())
                .size(body.size() == null ? 0L : body.size())
                .uploadedAt(Instant.now())
                .uploadedBy(uEmail)
                .adminOnly(Boolean.TRUE.equals(body.adminOnly()))
                .build();
        d = docs.save(d);
        return toRes(d);
    }

    @Operation(summary = "Xóa tài liệu theo id")
    @DeleteMapping("/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String empId, @PathVariable String docId) {
        var d = docs.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!empId.equals(d.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document does not belong to employee");
        }
        docs.deleteById(docId);
    }
}
