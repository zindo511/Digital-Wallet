package vn.huy.digital_wallet.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.huy.digital_wallet.common.WalletStatus;
import vn.huy.digital_wallet.dto.request.ChangePinRequest;
import vn.huy.digital_wallet.dto.request.SetPinRequest;
import vn.huy.digital_wallet.dto.response.WalletResponse;
import vn.huy.digital_wallet.exception.DuplicateResourceException;
import vn.huy.digital_wallet.exception.InvalidDataException;
import vn.huy.digital_wallet.exception.ResourceNotFoundException;
import vn.huy.digital_wallet.exception.WalletLockedException;
import vn.huy.digital_wallet.mapper.WalletMapper;
import vn.huy.digital_wallet.model.Wallet;
import vn.huy.digital_wallet.repository.WalletRepository;
import vn.huy.digital_wallet.service.WalletService;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINS = 30;

    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletMapper walletMapper; // MapStruct inject

    // ─────────────────────────────────────────
    // Public API methods
    // ─────────────────────────────────────────

    @Override
    public WalletResponse getInfo() {
        Wallet wallet = getCurrentWallet();
        return walletMapper.toResponse(wallet); // MapStruct thay thế fromEntity()
    }

    @Override
    public void setPin(SetPinRequest request) {
        if (!request.getPin().equals(request.getConfirmPin())) {
            throw new InvalidDataException("Yêu cầu PIN và ConfirmPin giống nhau");
        }

        Wallet wallet = getCurrentWallet();

        if (wallet.getPinHash() != null) {
            throw new DuplicateResourceException("Mã PIN đã tồn tại...Vui lòng sử dụng change-pin");
        }

        wallet.setPinHash(passwordEncoder.encode(request.getPin()));
        walletRepository.save(wallet);
    }

    @Override
    public void changePin(ChangePinRequest request) {
        Wallet wallet = getCurrentWallet();

        // Xác nhận PIN cũ (có kèm brute-force protection)
        verifyPin(wallet, request.getOldPin());

        if (!request.getNewPin().equals(request.getConfirmNewPin())) {
            throw new InvalidDataException("PIN mới và PIN xác nhận cần giống nhau");
        }

        wallet.setPinHash(passwordEncoder.encode(request.getNewPin()));
        walletRepository.save(wallet);
    }

    // ─────────────────────────────────────────
    // Private helper methods
    // ─────────────────────────────────────────

    /**
     * Lấy username từ SecurityContext của request đang thực thi.
     */
    private String getCurrentUsername() {
        return Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()).getName();
    }

    /**
     * Lấy Wallet của user đang đăng nhập; throw nếu không tìm thấy.
     */
    private Wallet getCurrentWallet() {
        return walletRepository.findByUser_Username(getCurrentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet không tồn tại"));
    }

    /**
     * Xác thực PIN nội bộ với cơ chế chống brute-force.
     * Gọi hàm này trước bất kỳ thao tác nào cần xác thực PIN.
     *
     * @param wallet Ví cần xác thực
     * @param rawPin PIN thô người dùng nhập vào
     * @throws WalletLockedException nếu ví đang bị khóa
     * @throws InvalidDataException  nếu PIN sai hoặc chưa được cài đặt
     */
    private void verifyPin(Wallet wallet, String rawPin) {
        // 1. Kiểm tra PIN đã được cài chưa
        if (wallet.getPinHash() == null) {
            throw new InvalidDataException("Vui lòng thiết lập mã PIN trước");
        }

        // 2. Kiểm tra ví có đang bị khóa không
        if (wallet.getStatus() == WalletStatus.LOCKED) {
            LocalDateTime lockedUntil = wallet.getPinLockedUntil();

            if (lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil)) {
                // Còn trong thời gian khóa → từ chối
                throw new WalletLockedException(
                        "Ví bị khóa do nhập sai PIN quá nhiều lần. Thử lại sau: " + lockedUntil);
            }

            // Đã hết thời gian khóa → tự động mở khóa
            wallet.setStatus(WalletStatus.ACTIVE);
            wallet.setPinFailedCount(0);
            wallet.setPinLockedUntil(null);
        }

        // 3. Kiểm tra PIN
        if (!passwordEncoder.matches(rawPin, wallet.getPinHash())) {
            int failedCount = wallet.getPinFailedCount() + 1;
            wallet.setPinFailedCount(failedCount);

            if (failedCount >= MAX_PIN_ATTEMPTS) {
                wallet.setStatus(WalletStatus.LOCKED);
                wallet.setPinLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINS));
                walletRepository.save(wallet);
                throw new WalletLockedException(
                        "Nhập sai PIN quá " + MAX_PIN_ATTEMPTS + " lần. Ví bị khóa trong " + LOCK_DURATION_MINS
                                + " phút.");
            }

            walletRepository.save(wallet);
            throw new InvalidDataException(
                    "Mã PIN không chính xác. Còn " + (MAX_PIN_ATTEMPTS - failedCount) + " lần thử.");
        }

        // 4. PIN đúng → reset counter
        wallet.setPinFailedCount(0);
        walletRepository.save(wallet);
    }
}
