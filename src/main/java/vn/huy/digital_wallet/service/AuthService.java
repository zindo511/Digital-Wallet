package vn.huy.digital_wallet.service;

import vn.huy.digital_wallet.dto.request.LoginRequest;
import vn.huy.digital_wallet.dto.request.RefreshTokenRequest;
import vn.huy.digital_wallet.dto.request.RegisterRequest;
import vn.huy.digital_wallet.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest registerRequest);

    AuthResponse login(LoginRequest loginRequest);

    AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    void logout(String accessToken, String refreshToken);
}
