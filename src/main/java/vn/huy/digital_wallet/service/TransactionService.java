package vn.huy.digital_wallet.service;

import org.springframework.data.domain.Page;
import vn.huy.digital_wallet.dto.request.DepositRequest;
import vn.huy.digital_wallet.dto.request.TransferRequest;
import vn.huy.digital_wallet.dto.response.TransactionResponse;

public interface TransactionService {

    // Chuyển tiền
    TransactionResponse transfer(String idempotencyKey, TransferRequest transferRequest, String ipAddress, String userAgent);

    // Lấy lịch sử giao dịch của 1 ví (có phân trang)
    Page<TransactionResponse> getHistory(int page, int size);

    // Lấy chi tiết 1 giao dịch
    TransactionResponse getById(Long id);

    // Deposit (Nạp tiền)
    TransactionResponse deposit(
            String idempotencyKey, DepositRequest depositRequest,
            String ipAddress, String userAgent
    );
}
