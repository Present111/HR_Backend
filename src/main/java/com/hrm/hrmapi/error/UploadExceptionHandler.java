import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class UploadExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE) // 413
    public Map<String,Object> handle(MaxUploadSizeExceededException e) {
        return Map.of(
                "message", "Dung lượng upload vượt giới hạn cho phép",
                "hint", "Giảm số/lượng file hoặc liên hệ admin tăng giới hạn"
        );
    }
}
