package vn.huy.digital_wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    // 1. Các trường chung
    private int code;
    private String message;
    private String timestamp;

    // 2. Trường của Success
    private T data;

    // 3. Trường của error
    private String error;
    private String path;

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .code(status.value())
                .message(message)
                .data(data)
                .timestamp(Instant.now().toString())
                .build();
    }

    // Overload trong trường hợp không cần truyền message
    public static <T> ApiResponse<T> success(HttpStatus status, T data) {
        return success(status, status.getReasonPhrase(), data);
    }

    public static <T> ResponseEntity<ApiResponse<T>> toResponseEntity(HttpStatus status, String message, T data) {
        ApiResponse<T> response = success(status, message, data);
        return ResponseEntity.status(status).body(response);
    }
}
