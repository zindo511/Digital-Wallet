package vn.huy.digital_wallet.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.huy.digital_wallet.common.Role;
import vn.huy.digital_wallet.config.JwtProperties;
import vn.huy.digital_wallet.config.JwtService;
import vn.huy.digital_wallet.config.UserDetailsImpl;
import vn.huy.digital_wallet.dto.request.LoginRequest;
import vn.huy.digital_wallet.dto.request.RegisterRequest;
import vn.huy.digital_wallet.dto.response.AuthResponse;
import vn.huy.digital_wallet.model.User;
import vn.huy.digital_wallet.model.Wallet;
import vn.huy.digital_wallet.repository.UserRepository;
import vn.huy.digital_wallet.repository.WalletRepository;
import vn.huy.digital_wallet.service.AuthService;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService { // register, login, refresh, logout

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    // ĐĂNG KÝ
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1.Kiểm tra xem username hoặc email đã ồn tại chưa
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã được sử dụng"); // để đây chút dùng global exception
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        // Tạo User mới
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // mã hoá mật khẩu
        user.setRole(Role.USER);

        // Lưu user vào DB
        User savedUser = userRepository.save(user);

        // Tạo tự động wallet cho User vừa đăng ký
        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        walletRepository.save(wallet);

        // Bọc User vào UserDetailsImpl để tạo token
        UserDetailsImpl userDetails = new UserDetailsImpl(savedUser);

        // Trả về Access Token và Refresh Token ngay khi đăng ký thành công
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(userDetails))
                .refreshToken(jwtService.generateRefreshToken(userDetails))
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        return null;
    }
}
