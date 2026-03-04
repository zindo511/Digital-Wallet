package vn.huy.digital_wallet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.huy.digital_wallet.dto.ApiResponse;
import vn.huy.digital_wallet.dto.request.ChangePinRequest;
import vn.huy.digital_wallet.dto.request.SetPinRequest;
import vn.huy.digital_wallet.dto.response.WalletResponse;
import vn.huy.digital_wallet.service.WalletService;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * GET /api/wallets/me
     * Trả về thông tin ví (số dư, trạng thái) của user đang đăng nhập.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet() {
        WalletResponse data = walletService.getInfo();
        return ApiResponse.toResponseEntity(HttpStatus.OK, "Lấy thông tin ví thành công", data);
    }

    @PostMapping("/set-pin")
    public ResponseEntity<ApiResponse<Void>> setPin(@Valid @RequestBody SetPinRequest request) {
        walletService.setPin(request);
        return ApiResponse.toResponseEntity(HttpStatus.CREATED, "Cài đặt PIN thành công", null);
    }

    @PostMapping("/change-pin")
    public ResponseEntity<ApiResponse<Void>> changePin(@Valid @RequestBody ChangePinRequest request) {
        walletService.changePin(request);
        return ApiResponse.toResponseEntity(HttpStatus.NO_CONTENT, "Đổi PIN thành công", null);
    }
}
