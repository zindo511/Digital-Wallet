package vn.huy.digital_wallet.service.Impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.huy.digital_wallet.common.Role;
import vn.huy.digital_wallet.config.JwtProperties;
import vn.huy.digital_wallet.config.JwtService;
import vn.huy.digital_wallet.config.UserDetailsImpl;
import vn.huy.digital_wallet.dto.request.LoginRequest;
import vn.huy.digital_wallet.dto.request.RefreshTokenRequest;
import vn.huy.digital_wallet.dto.request.RegisterRequest;
import vn.huy.digital_wallet.dto.response.AuthResponse;
import vn.huy.digital_wallet.model.User;
import vn.huy.digital_wallet.model.Wallet;
import vn.huy.digital_wallet.repository.UserRepository;
import vn.huy.digital_wallet.repository.WalletRepository;
import vn.huy.digital_wallet.service.AuthService;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService { // register, login, refresh, logout

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate; // đọc/ghi dữ liệu vào Redis từ code Java
    private static final String REFRESH_TOKEN_PREFIX = "rt:"; // nhãn dán
    private static final String BLACKLIST_PREFIX = "blacklist:";

    // --- ĐĂNG KÝ ---
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1.Kiểm tra xem username hoặc email đã tồn tại chưa
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

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Lưu refresh token vào Redis whitelist
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getUsername(),
                refreshToken,
                jwtProperties.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        // Trả về Access Token và Refresh Token ngay khi đăng ký thành công
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                .build();
    }

    // -- ĐĂNG NHẬP ---
    @Override
    public AuthResponse login(LoginRequest request) {

        // Uỷ quyền cho Spring Security kiểm tra tài khoản & mật khẩu
        // Nếu sai mật khẩu, nó sẽ tự văng lỗi (BadCredentialsException), code bên dưới
        // không chạy
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));
        // Nếu xác thực thành công, lấy User từ DB ra
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));

        // Bọc User vào UserDetailsImpl
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Lưu refresh token vào Redis whitelist
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getUsername(),
                refreshToken,
                jwtProperties.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        // Trả về Access Token và Refresh Token ngay khi đăng nhập thành công
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                .build();
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        // 1. Giải mã lấy username từ refresh token
        String username = jwtService.extractUsername(refreshToken);

        // 2. Load user từ DB
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        // 3. Kiểm tra refresh token có hợp lệ không
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        // Kiểm tra token có trong Redis whitelist không
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + username);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new RuntimeException("Refresh token đã bị thu hồi, vui lòng đăng nhập lại");
        }

        // 4. Cấp access token mới
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(userDetails))
                .refreshToken(refreshToken) // Giữ nguyên refresh token cũ
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                .build();
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        // 1. Blacklist access token (tự xoá khi hết hạn)
        long ttl = jwtService.extractExpiration(accessToken).getTime() -
                System.currentTimeMillis();
        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken,
                    "true",
                    ttl,
                    TimeUnit.MILLISECONDS
            );
        }

        // 2. Xoá refresh token khỏi white list -> vô hiệu hoá ngay lập tức
        String username = jwtService.extractUsername(refreshToken);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + username);
    }
}
