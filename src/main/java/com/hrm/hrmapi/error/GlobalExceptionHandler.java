package com.hrm.hrmapi.error;

import com.mongodb.DuplicateKeyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ===== Lỗi validate @Valid ở @RequestBody ===== */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest req) {
        var status = HttpStatus.BAD_REQUEST;
        var pd = ProblemFactory.of(status, "Validation failed", "Body validation errors", req, "VALIDATION_ERROR");

        // gom lỗi từng field
        List<Map<String, Object>> errors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            var item = new LinkedHashMap<String, Object>();
            item.put("field", fe.getField());
            item.put("message", fe.getDefaultMessage());
            item.put("rejectedValue", fe.getRejectedValue());
            errors.add(item);
        }
        pd.setProperty("errors", errors);
        return ResponseEntity.status(status).body(pd);
    }

    /* ===== Lỗi validate @RequestParam / @PathVariable ===== */
    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ProblemDetail> handleConstraint( Exception ex,
                                                           HttpServletRequest req) {
        var status = HttpStatus.BAD_REQUEST;
        var pd = ProblemFactory.of(status, "Constraint violation", ex.getMessage(), req, "CONSTRAINT_VIOLATION");
        return ResponseEntity.status(status).body(pd);
    }

    /* ===== JSON parse sai, thiếu field, kiểu dữ liệu không khớp ===== */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest req) {
        var status = HttpStatus.BAD_REQUEST;
        var pd = ProblemFactory.of(status, "Malformed JSON", ex.getMostSpecificCause().getMessage(), req, "MALFORMED_JSON");
        return ResponseEntity.status(status).body(pd);
    }

    /* ===== Lỗi ném bằng ResponseStatusException (mày dùng nhiều trong controller) ===== */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleRSE(ResponseStatusException ex, HttpServletRequest req) {
        var status = ex.getStatusCode() instanceof HttpStatus hs ? hs : HttpStatus.valueOf(ex.getStatusCode().value());
        var pd = ProblemFactory.of(status, ex.getReason(), ex.getReason(), req, "ERROR");
        return ResponseEntity.status(status).body(pd);
    }

    /* ===== Lỗi nghiệp vụ của mày ===== */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex, HttpServletRequest req) {
        var status = ex.getStatus();
        var pd = ProblemFactory.of(status, "Business error", ex.getMessage(), req, ex.getCode());
        return ResponseEntity.status(status).body(pd);
    }

    /* ===== Mongo duplicate key (unique index: email, code, employeeId…) ===== */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ProblemDetail> handleDupKey(DuplicateKeyException ex, HttpServletRequest req) {
        var status = HttpStatus.CONFLICT;
        var message = "Duplicate key"; // có thể parse ex.getMessage() để biết field nào trùng
        var pd = ProblemFactory.of(status, "Duplicate key", message, req, "DUPLICATE_KEY");
        return ResponseEntity.status(status).body(pd);
    }

    /* ===== Không đủ quyền (đã xác thực nhưng bị chặn) ===== */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        var status = HttpStatus.FORBIDDEN;
        var pd = ProblemFactory.of(status, "Forbidden", ex.getMessage(), req, "FORBIDDEN");
        return ResponseEntity.status(status).body(pd);
    }

    /* ===== Chưa xác thực / token lỗi ===== */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        var status = HttpStatus.UNAUTHORIZED;
        var pd = ProblemFactory.of(status, "Unauthorized", ex.getMessage(), req, "UNAUTHORIZED");
        return ResponseEntity.status(status).body(pd);
    }

    /* ===== Trả về lỗi theo ErrorResponse/ProblemDetail mặc định của Spring ===== */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ProblemDetail> handleErrorResponse(ErrorResponseException ex, HttpServletRequest req) {
        ProblemDetail pd = ex.getBody();
        // bổ sung metadata nếu muốn
        pd.setProperty("path", req.getRequestURI());
        return ResponseEntity.status(ex.getStatusCode()).body(pd);
    }

    /* ===== Còn lại: Internal Server Error ===== */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleOther(Exception ex, HttpServletRequest req) {
        var status = HttpStatus.INTERNAL_SERVER_ERROR;
        var pd = ProblemFactory.of(status, "Internal Server Error",
                "Unexpected error occurred", req, "INTERNAL_ERROR");
        // Log chi tiết server-side; không trả stack trace cho client
        ex.printStackTrace();
        return ResponseEntity.status(status).body(pd);
    }
}
