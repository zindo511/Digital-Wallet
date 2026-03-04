package vn.huy.digital_wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SetPinRequest {

    @NotBlank(message = "PIN không được để trống")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN phải là 6 chữ số")
    private String pin;

    @NotBlank(message = "Vui lòng xác nhận PIN")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN phải là 6 chữ số")
    private String confirmPin;
}
