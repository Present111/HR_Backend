package com.hrm.hrmapi.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("docs")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Doc {
    @Id private String id;

    @Indexed private String employeeId;      // tài liệu thuộc nhân viên nào
    @Indexed private String type;            // CONTRACT, ID, CERT, OTHER (tự do string để linh hoạt)

    private String name;                     // tên hiển thị cho người dùng
    private String url;                      // nơi lưu file (S3, GCS, local…); mock cũng để đây
    private long   size;                     // bytes (tùy chọn)
    private String mime;                     // "application/pdf"…

    private Instant uploadedAt;
    private String  uploadedBy;              // email/id người upload

    // quyền xem: ADMIN sẽ luôn thấy; các role khác có thể bị ẩn (ví dụ CONTRACT)
    private boolean adminOnly;               // true: chỉ ADMIN mới thấy
}
