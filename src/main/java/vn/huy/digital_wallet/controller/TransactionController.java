package vn.huy.digital_wallet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.huy.digital_wallet.dto.ApiResponse;
import vn.huy.digital_wallet.dto.request.TransferRequest;
import vn.huy.digital_wallet.dto.response.TransactionResponse;
import vn.huy.digital_wallet.service.TransactionService;

@RestController
@RequestMapping("/api/transsactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/transactions/transfer
     * Chuyển tiền — yêu cầu header X-Idempotency-Key
     */
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        TransactionResponse response = transactionService.transfer(idempotencyKey, request);
        return ApiResponse.toResponseEntity(HttpStatus.OK, "Chuyển tiền thành công", response);
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TransactionResponse> data = transactionService.getHistory(page, size);
        return ApiResponse.toResponseEntity(HttpStatus.OK, "Lấy lịch sử thành công", data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(@PathVariable Long id) {
        TransactionResponse data = transactionService.getById(id);
        return ApiResponse.toResponseEntity(HttpStatus.OK, "Lấy chi tiết giao dịch thành công", data);
    }
}
