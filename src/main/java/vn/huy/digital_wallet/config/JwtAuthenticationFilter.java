package vn.huy.digital_wallet.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter { // "người lính gác cổng", chặn ở mọi API
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lấy header Authorization từ request
        final String authorizationHeader = request.getHeader("Authorization");
    }
}
/*
Lấy Token từ Header ->
Giải mã lấy Username ->
Kiểm tra tính hợp lệ ->
Nếu OK thì cấp quyền cho user đi tiếp vào hệ thống.
 */
