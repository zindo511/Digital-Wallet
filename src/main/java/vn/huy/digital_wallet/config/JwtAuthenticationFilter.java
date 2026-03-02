package vn.huy.digital_wallet.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter { // "người lính gác cổng", chặn ở mọi API

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lấy header Authorization từ request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 2. Kiểm tra xem header có chứa token không (Token phải bắt đầu bằng chữ "Bearer ")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Không có token hợp lệ thì cho đi tiếp (tới các API public hoặc bị Spring Security chặn lại báo lỗi 403)
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Cắt bỏ chữ "Bearer "(7 ký tự) để lấy chuỗi token thật sự
        jwt = authHeader.substring(7);

        // 4. Giải mã token để lấy username
        try {
            username = jwtService.extractUsername(jwt);

            // 5. Nếu lấy được username và người dùng này chưa xác thực trong SecurityContext
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Móc xuống DB để lấy thông tin User (thông qua UserDetailsServiceImpl)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // 6. Nhờ JwtService kiểm tra xem Token có còn hạn và khớp với user này không
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Nếu Token hoàn hảo -> Tạo đối tượng xác thực (chứa thông tin user và quyền hạn)
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Không cần truyền password vào đây cho an toàn
                            userDetails.getAuthorities()
                    );

                    // Ghi nhận chi tiết request (IP, session...)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 7. Lưu đối tượng xác thực vào SecurityContextHolder (Cấp thẻ xanh cho qua cửa)
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (Exception e) {
            // Nếu Token bị sai chữ ký, hết hạn, hoặc bị can thiệp -> Bỏ qua, không cấp thẻ xanh
            logger.error("Lỗi xác thực JWT Token: " + e.getMessage());
        }

        // 8. Cho request đi tiếp đến đích (Controller)
        filterChain.doFilter(request, response);
    }
}
/*
Lấy Token từ Header ->
Giải mã lấy Username ->
Kiểm tra tính hợp lệ ->
Nếu OK thì cấp quyền cho user đi tiếp vào hệ thống.
 */
