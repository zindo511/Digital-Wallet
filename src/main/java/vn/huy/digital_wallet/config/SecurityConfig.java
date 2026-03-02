package vn.huy.digital_wallet.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
     private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF vì chúng ta dùng Token (Stateless), không dùng Cookie/Session
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Phân quyền các đường dẫn (Endpoint)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Thả cửa cho Đăng ký, Đăng nhập, Refresh Token
                        .anyRequest().authenticated() // Bắt buộc phải có Token hợp lệ cho tất cả API còn lại
                )

                // 3. Cấu hình Session: Không lưu trạng thái (STATELESS)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Khai báo AuthenticationProvider (Cung cấp logic xác thực)
                .authenticationProvider(authenticationProvider())

                // 5. Thêm JWT Filter đứng trước UsernamePasswordAuthenticationFilter mặc định
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        ;
        return http.build();
    }

    // --- Các Bean hỗ trợ xác thực ---

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Khai báo dịch vụ tìm User của bạn
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder()); // Khai báo bộ mã hóa mật khẩu
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        // Expose AuthenticationManager ra để lát nữa dùng trong AuthService (API Login)
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Sử dụng BCrypt để băm mật khẩu (Tiêu chuẩn an toàn nhất hiện nay)
        return new BCryptPasswordEncoder();
    }

}
