package vn.huy.digital_wallet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.huy.digital_wallet.common.WalletStatus;
import vn.huy.digital_wallet.exception.InvalidDataException;
import vn.huy.digital_wallet.exception.WalletLockedException;
import vn.huy.digital_wallet.model.Wallet;
import vn.huy.digital_wallet.repository.WalletRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PinVerificationService {

    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINS = 30;

    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;

    /**
     * Xác thực PIN nội bộ với cơ chế chống brute-force.
     * Gọi hàm này trước bất kỳ thao tác nào cần xác thực PIN.
     *
     * @param wallet Ví cần xác thực
     * @param rawPin PIN thô người dùng nhập vào
     * @throws WalletLockedException nếu ví đang bị khóa
     * @throws InvalidDataException  nếu PIN sai hoặc chưa được cài đặt
     */
    public void verifyPin(Wallet wallet, String rawPin) {
        if (wallet.getPinHash() == null) {
            throw new InvalidDataException("Vui lòng thiết lập mã PIN trước");
        }
        if (wallet.getStatus() == WalletStatus.LOCKED) {
            LocalDateTime lockedUntil = wallet.getPinLockedUntil();
            if (lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil)) {
                throw new WalletLockedException(
                        "Ví bị khóa do nhập sai PIN quá nhiều lần. Thử lại sau: " + lockedUntil);
            }
            // Hết thời gian khóa → tự mở khóa
            wallet.setStatus(WalletStatus.ACTIVE);
            wallet.setPinFailedCount(0);
            wallet.setPinLockedUntil(null);
        }
        if (!passwordEncoder.matches(rawPin, wallet.getPinHash())) {
            int failedCount = wallet.getPinFailedCount() + 1;
            wallet.setPinFailedCount(failedCount);
            if (failedCount >= MAX_PIN_ATTEMPTS) {
                wallet.setStatus(WalletStatus.LOCKED);
                wallet.setPinLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINS));
                walletRepository.save(wallet);
                throw new WalletLockedException("Nhập sai PIN quá 5 lần. Ví bị khóa trong 30 phút.");
            }
            walletRepository.save(wallet);
            throw new InvalidDataException(
                    "Mã PIN không chính xác. Còn " + (MAX_PIN_ATTEMPTS - failedCount) + " lần thử.");
        }
        wallet.setPinFailedCount(0);
        walletRepository.save(wallet);
    }
}
