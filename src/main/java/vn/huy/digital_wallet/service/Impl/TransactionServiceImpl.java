package vn.huy.digital_wallet.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.huy.digital_wallet.common.TransactionStatus;
import vn.huy.digital_wallet.common.TransactionType;
import vn.huy.digital_wallet.common.WalletStatus;
import vn.huy.digital_wallet.dto.request.TransferRequest;
import vn.huy.digital_wallet.dto.response.TransactionResponse;
import vn.huy.digital_wallet.exception.InvalidDataException;
import vn.huy.digital_wallet.exception.ResourceNotFoundException;
import vn.huy.digital_wallet.exception.WalletLockedException;
import vn.huy.digital_wallet.mapper.TransactionMapper;
import vn.huy.digital_wallet.model.Transaction;
import vn.huy.digital_wallet.model.Wallet;
import vn.huy.digital_wallet.repository.TransactionRepository;
import vn.huy.digital_wallet.repository.WalletRepository;
import vn.huy.digital_wallet.service.DistributedLockService;
import vn.huy.digital_wallet.service.IdempotencyService;
import vn.huy.digital_wallet.service.PinVerificationService;
import vn.huy.digital_wallet.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final WalletRepository walletRepository;
    private final PinVerificationService pinVerificationService;
    private final DistributedLockService distributedLockService;
    private final PasswordEncoder passwordEncoder;
    private IdempotencyService idempotencyService;

    @Override
    public TransactionResponse transfer(String idempotencyKey, TransferRequest request) {

        // Idempotency: kiểm tra request này đã xử lý chưa
        idempotencyService.checkAndMark(idempotencyKey);

        // Load dữ liệu vào
        Wallet senderWallet = getCurrentWallet();
        Wallet receiverWallet = walletRepository.findById(request.getToWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Ví nhận không tồn tại"));

        // Validate nghiệp vụ

        // Không chuyển tiền cho chính mình
        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new InvalidDataException("Không thể chuyển tiền cho chính mình");
        }

        // Ví gửi phải ACTIVE
        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletLockedException("Ví của bạn đang bị khoá");
        }

        // Ví nhận phải ACTIVE
        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidDataException("Ví nhận đang bị khóa, không thể nhận tiền");
        }

        // Xác thực PIN
        pinVerificationService.verifyPin(senderWallet, request.getPin());

        // --- Distributed Lock ---
        String lockValue = UUID.randomUUID().toString();
        boolean lockAcquired = distributedLockService.tryLock(senderWallet.getId(),  lockValue);

        if (!lockAcquired) {
            throw new InvalidDataException("Ví đang có giao dịch khác đang xử lý, vui lòng thử lại");
        }

        TransactionResponse response;
        try {
            // Thực hiện giao dịch
            response = executeTransfer(senderWallet, receiverWallet, request, idempotencyKey);
        } finally {
            // Giải phóng lock dù thành công hay lỗi
            distributedLockService.releaseLock(senderWallet.getId(), lockValue);
        }
        return response;
    }

    @Override
    public Page<TransactionResponse> getHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Wallet wallet = getCurrentWallet();
        Page<Transaction> transactions = transactionRepository.findBySourceWallet_IdOrDestinationWallet_IdOrderByCreatedAtDesc(
                wallet.getId(), wallet.getId(), pageable
        );

        return transactions.map(transactionMapper::toResponse);
    }

    @Override
    public TransactionResponse getById(Long id) {
        Wallet wallet = getCurrentWallet();
        Transaction transaction = transactionRepository.findByIdAndSourceWallet_IdOrIdAndDestinationWallet_Id(
                id, wallet.getId(),
                id, wallet.getId()
        ).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch tương ứng với ví"));
        return transactionMapper.toResponse(transaction);
    }

    // --- private helper method ---
    private String getCurrentUsername() {
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                .getName();
    }

    private Wallet getCurrentWallet() {
        return walletRepository.findByUser_Username(getCurrentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }

    @Transactional
    protected TransactionResponse executeTransfer(
            Wallet senderWallet,
            Wallet receiverWallet,
            TransferRequest request,
            String idempotencyKey
    ) {

        BigDecimal amount = request.getAmount();

        // Ghi nhớ số dư trước giao dịch
        BigDecimal balanceBefore = senderWallet.getBalance();

        // Trừ ví gửi
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));

        // Cộng ví nhận
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));

        // Lưu cả 2 ví
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        // Tạo transaction record
        Transaction transaction = new Transaction();
        transaction.setTransactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setSourceWallet(senderWallet);
        transaction.setDestinationWallet(receiverWallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setDescription(request.getDescription());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(senderWallet.getBalance());
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setFee(BigDecimal.ZERO);
        Transaction saved = transactionRepository.save(transaction);

        // Lưu kết quả vào idempotency store
        TransactionResponse response = transactionMapper.toResponse(saved);
        idempotencyService.saveResult(idempotencyKey, saved.getId().toString());

        log.info("Transfer thành công: {} → {}, amount={}",
                senderWallet.getId(), receiverWallet.getId(), amount);
        return response;
    }
}
