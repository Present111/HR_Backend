package com.hrm.hrmapi.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public class ProblemFactory {

    public static ProblemDetail of(HttpStatus status, String title, String detail,
                                   HttpServletRequest req, String code) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail == null ? "" : detail);
        pd.setTitle(title == null ? status.getReasonPhrase() : title);
        pd.setType(URI.create("about:blank")); // có thể trỏ về trang docs mã lỗi của mày
        pd.setProperty("timestamp", Instant.now().toString());
        if (req != null) {
            pd.setProperty("path", req.getRequestURI());
            pd.setProperty("method", req.getMethod());
        }
        if (code != null) pd.setProperty("code", code);
        // gợi ý traceId; có thể thay bằng MDC/Observability thực tế
        pd.setProperty("traceId", UUID.randomUUID().toString());
        return pd;
    }
}
