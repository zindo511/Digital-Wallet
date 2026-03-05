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
import vn.huy.digital_wallet.service.PinVerificationService;
import vn.huy.digital_wallet.service.WalletService;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletMapper walletMapper; // MapStruct inject
    private final PinVerificationService pinVerificationService;

    // ─────────────────────────────────────────
    // Public API methods
    // ─────────────────────────────────────────

    @Override
    public WalletResponse getInfo() {
        Wallet wallet = getCurrentWallet();
        return walletMapper.toResponse(wallet);
    }

    @Override
    public void setPin(SetPinRequest request) {
        // 1. Kiểm tra pin và confirmPin có khớp nhau không
        if (!request.getPin().equals(request.getConfirmPin())) {
            throw new InvalidDataException("Mã PIN và xác nhận PIN không khớp");
        }

        Wallet wallet = getCurrentWallet();

        // 2. Ví phải đang ACTIVE mới được phép đặt PIN
        if (wallet.getStatus() == WalletStatus.LOCKED) {
            throw new WalletLockedException("Ví đang bị khóa, không thể thực hiện thao tác này");
        }

        // 3. Kiểm tra PIN đã được đặt chưa (tránh ghi đè)
        if (wallet.getPinHash() != null) {
            throw new DuplicateResourceException("Mã PIN đã tồn tại. Vui lòng dùng API đổi PIN");
        }

        // 4. Hash PIN bằng BCrypt và lưu
        wallet.setPinHash(passwordEncoder.encode(request.getPin()));
        walletRepository.save(wallet);
    }

    @Override
    public void changePin(ChangePinRequest request) {
        Wallet wallet = getCurrentWallet();

        // Xác nhận PIN cũ
        pinVerificationService.verifyPin(wallet, request.getOldPin());

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

}
