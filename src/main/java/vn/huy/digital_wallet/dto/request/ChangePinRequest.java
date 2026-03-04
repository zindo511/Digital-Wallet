package vn.huy.digital_wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePinRequest {

    @NotBlank(message = "Cần nhập mã PIN cũ")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN phải là 6 chữ số")
    private String oldPin;

    @NotBlank(message = "Cần nhập mã PIN mới")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN phải là 6 chữ số")
    private String newPin;

    @NotBlank(message = "Cần xác nhận mã PIN mới")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN phải là 6 chữ số")
    private String confirmNewPin;
}
