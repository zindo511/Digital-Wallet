package vn.huy.digital_wallet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.huy.digital_wallet.dto.ApiResponse;
import vn.huy.digital_wallet.dto.request.LoginRequest;
import vn.huy.digital_wallet.dto.request.RefreshTokenRequest;
import vn.huy.digital_wallet.dto.request.RegisterRequest;
import vn.huy.digital_wallet.dto.response.AuthResponse;
import vn.huy.digital_wallet.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // --- ĐĂNG KÝ ---
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse authResponse = authService.register(registerRequest);

        return ApiResponse.toResponseEntity(HttpStatus.CREATED, "User đã được tạo thành công", authResponse);
    }

    // --- ĐĂNG NHẬP ---
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return ApiResponse.toResponseEntity(HttpStatus.ACCEPTED, "User đã đăng nhập thành công", authResponse);
    }

    // --- REFRESH TOKEN ---
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        AuthResponse authResponse = authService.refreshToken(refreshTokenRequest);
        return ApiResponse.toResponseEntity(HttpStatus.CREATED, "Tạo mới access token thành công", authResponse);
    }

    // --- LOGOUT ---
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        String accessToken = authHeader.substring(7);
        authService.logout(accessToken, request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
