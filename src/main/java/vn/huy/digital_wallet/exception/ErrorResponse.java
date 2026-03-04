package vn.huy.digital_wallet.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Ẩn các trường có giá trị null khi chuyển thành Json
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String path;
    private String message;

    // Lưu trữ danh sách lỗi của từng ô nhập liệu
    private Map<String, List<String>> validationErrors;
}
