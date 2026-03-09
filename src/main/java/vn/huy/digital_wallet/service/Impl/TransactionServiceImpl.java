package vn.huy.digital_wallet.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import vn.huy.digital_wallet.common.IdempotencyResult;
import vn.huy.digital_wallet.common.TransactionStatus;
import vn.huy.digital_wallet.common.TransactionType;
import vn.huy.digital_wallet.common.WalletStatus;
import vn.huy.digital_wallet.dto.request.DepositRequest;
import vn.huy.digital_wallet.dto.request.TransferRequest;
import vn.huy.digital_wallet.dto.response.TransactionResponse;
import vn.huy.digital_wallet.event.TransactionCompletedEvent;
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
    private final IdempotencyService idempotencyService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TransactionResponse transfer(
            String idempotencyKey, TransferRequest request,
            String ipAddress, String userAgent) {

        // 1. Idempotency: chặn request trùng lặp
        IdempotencyResult idempotency = idempotencyService.checkAndMark(idempotencyKey);

        return switch (idempotency.status()) {
            case COMPLETED -> objectMapper.readValue(idempotency.payload(), TransactionResponse.class);
            case IN_PROGRESS -> throw new InvalidDataException("Giao dịch đang xử lý, vui lòng chờ");
            case FAILED -> throw new InvalidDataException("Request đã thất bại: " + idempotency.payload());
            case NEW -> {
                // 2. Validate & load dữ liệu
                Wallet sourceWallet = getCurrentWallet();

                Wallet destinationWallet = walletRepository.findById(request.getToWalletId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ví người nhận không tồn tại"));

                if (sourceWallet.getId().equals(destinationWallet.getId())) {
                    throw new InvalidDataException("Không thể tự chuyển tiền cho chính mình");
                }

                if (sourceWallet.getStatus() != WalletStatus.ACTIVE) {
                    throw new WalletLockedException("Ví của bạn đang bị khoá");
                }

                if (sourceWallet.getBalance().compareTo(request.getAmount()) < 0) {
                    throw new InvalidDataException("Số dư không đủ để thực hiện giao dịch");
                }

                pinVerificationService.verifyPin(sourceWallet, request.getPin());

                // 3. Distributed Lock — khóa theo thứ tự ID nhỏ trước để tránh deadlock
                Long firstId = Math.min(sourceWallet.getId(), destinationWallet.getId());
                Long secondId = Math.max(sourceWallet.getId(), destinationWallet.getId());

                String lockValue1 = UUID.randomUUID().toString();
                String lockValue2 = UUID.randomUUID().toString();

                boolean lock1 = distributedLockService.tryLock(firstId, lockValue1);
                if (!lock1) {
                    idempotencyService.saveResult(idempotencyKey, "FAILED:Ví đang bận");
                    throw new InvalidDataException("Ví đang có giao dịch khác, vui lòng thử lại");
                }

                boolean lock2 = distributedLockService.tryLock(secondId, lockValue2);
                if (!lock2) {
                    distributedLockService.releaseLock(firstId, lockValue1);
                    idempotencyService.saveResult(idempotencyKey, "FAILED:Ví người nhận đang bận");
                    throw new InvalidDataException("Ví người nhận đang bận, vui lòng thử lại");
                }

                try {
                    // 4. Thực hiện giao dịch: trừ/cộng tiền
                    BigDecimal srcBefore = sourceWallet.getBalance();
                    BigDecimal dstBefore = destinationWallet.getBalance();

                    sourceWallet.setBalance(srcBefore.subtract(request.getAmount()));
                    destinationWallet.setBalance(dstBefore.add(request.getAmount()));

                    walletRepository.save(sourceWallet);
                    walletRepository.save(destinationWallet);

                    // 5. Tạo và lưu bản ghi giao dịch
                    Transaction txn = new Transaction();
                    txn.setTransactionReference("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    txn.setSourceWallet(sourceWallet);
                    txn.setDestinationWallet(destinationWallet);
                    txn.setAmount(request.getAmount());
                    txn.setType(TransactionType.TRANSFER);
                    txn.setStatus(TransactionStatus.SUCCESS);
                    txn.setIdempotencyKey(idempotencyKey);
                    txn.setDescription(request.getDescription() != null
                            ? request.getDescription()
                            : "Chuyển tiền");
                    txn.setBalanceBefore(srcBefore);
                    txn.setBalanceAfter(sourceWallet.getBalance());
                    txn.setFee(BigDecimal.ZERO);
                    txn.setCompletedAt(LocalDateTime.now());

                    Transaction saved = transactionRepository.save(txn);

                    // 6. Đánh dấu idempotency COMPLETED
                    TransactionResponse response = transactionMapper.toResponse(saved);
                    idempotencyService.saveResult(idempotencyKey,
                            "COMPLETED:" + objectMapper.writeValueAsString(response));

                    // 7. Phát event bất đồng bộ (notification, audit log)
                    eventPublisher.publishEvent(
                            new TransactionCompletedEvent(saved, sourceWallet.getUser(), ipAddress, userAgent));

                    log.info("Transfer thành công: srcWallet={}, dstWallet={}, amount={}",
                            sourceWallet.getId(), destinationWallet.getId(), request.getAmount());

                    yield response;

                } catch (Exception e) {
                    idempotencyService.saveResult(idempotencyKey, "FAILED:" + e.getMessage());
                    throw e;
                } finally {
                    // Luôn release lock dù thành công hay thất bại
                    distributedLockService.releaseLock(firstId, lockValue1);
                    distributedLockService.releaseLock(secondId, lockValue2);
                }
            }
        };
    }

    @Override
    public Page<TransactionResponse> getHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Wallet wallet = getCurrentWallet();
        Page<Transaction> transactions = transactionRepository
                .findBySourceWallet_IdOrDestinationWallet_IdOrderByCreatedAtDesc(
                        wallet.getId(), wallet.getId(), pageable);

        return transactions.map(transactionMapper::toResponse);
    }

    @Override
    public TransactionResponse getById(Long id) {
        Wallet wallet = getCurrentWallet();
        Transaction transaction = transactionRepository.findByIdAndSourceWallet_IdOrIdAndDestinationWallet_Id(
                id, wallet.getId(),
                id, wallet.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch tương ứng với ví"));
        return transactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse deposit(String idempotencyKey, DepositRequest depositRequest,
            String ipAddress, String userAgent) {

        // 1. Idempotency: chặn request trùng lặp
        IdempotencyResult idempotency = idempotencyService.checkAndMark(idempotencyKey);

        return switch (idempotency.status()) {
            case COMPLETED -> objectMapper.readValue(idempotency.payload(), TransactionResponse.class);
            case IN_PROGRESS -> throw new InvalidDataException("Giao dịch đang xử lý, vui lòng chờ");
            case FAILED -> throw new InvalidDataException("Request đã thất bại: " + idempotency.payload());
            case NEW -> {
                // 2. Load & validate ví
                Wallet wallet = getCurrentWallet();

                if (wallet.getStatus() != WalletStatus.ACTIVE) {
                    throw new WalletLockedException("Ví của bạn đang bị khoá");
                }

                // 3. Distributed Lock
                String lockValue = UUID.randomUUID().toString();
                boolean lockAcquired = distributedLockService.tryLock(wallet.getId(), lockValue);
                if (!lockAcquired) {
                    idempotencyService.saveResult(idempotencyKey, "FAILED:Ví đang bận");
                    throw new InvalidDataException("Ví đang có giao dịch khác đang xử lý, vui lòng thử lại sau");
                }

                try {
                    // 4. Cộng tiền vào ví
                    BigDecimal balanceBefore = wallet.getBalance();
                    wallet.setBalance(wallet.getBalance().add(depositRequest.getAmount()));
                    walletRepository.save(wallet);

                    // 5. Tạo và lưu transaction
                    Transaction transaction = new Transaction();
                    transaction.setTransactionReference(
                            "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    transaction.setDestinationWallet(wallet);
                    transaction.setSourceWallet(null);
                    transaction.setAmount(depositRequest.getAmount());
                    transaction.setType(TransactionType.DEPOSIT);
                    transaction.setStatus(TransactionStatus.SUCCESS);
                    transaction.setIdempotencyKey(idempotencyKey);
                    transaction.setDescription(depositRequest.getDescription() != null
                            ? depositRequest.getDescription()
                            : "Nạp tiền vào ví");
                    transaction.setBalanceBefore(balanceBefore);
                    transaction.setBalanceAfter(wallet.getBalance());
                    transaction.setFee(BigDecimal.ZERO);
                    transaction.setCompletedAt(LocalDateTime.now());

                    Transaction saved = transactionRepository.save(transaction);

                    // 6. Đánh dấu idempotency COMPLETED
                    TransactionResponse response = transactionMapper.toResponse(saved);
                    idempotencyService.saveResult(idempotencyKey,
                            "COMPLETED:" + objectMapper.writeValueAsString(response));

                    // 7. Phát event bất đồng bộ
                    eventPublisher.publishEvent(
                            new TransactionCompletedEvent(saved, wallet.getUser(), ipAddress, userAgent));

                    log.info("Deposit thành công: walletId={}, amount={}", wallet.getId(), depositRequest.getAmount());
                    yield response;

                } catch (Exception e) {
                    idempotencyService.saveResult(idempotencyKey, "FAILED:" + e.getMessage());
                    throw e;
                } finally {
                    distributedLockService.releaseLock(wallet.getId(), lockValue);
                }
            }
        };
    }

    // --- PRIVATE HELPER METHOD ---
    private String getCurrentUsername() {
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                .getName();
    }

    private Wallet getCurrentWallet() {
        return walletRepository.findByUser_Username(getCurrentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }

}
