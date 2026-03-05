package vn.huy.digital_wallet.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "PIN không được để trống")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN phải là 6 chữ số")
    private String pin;

    @NotNull(message = "Ví nhận không được để trống")
    private Long toWalletId;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "2000", message = "Số tền tối thiểu là 2,000đ")
    @Digits(integer = 17, fraction = 2, message = "Số tiền không hợp lệ")
    private BigDecimal amount;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    private String description;
}
