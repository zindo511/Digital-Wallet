package vn.huy.digital_wallet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt") // Tự động tìm cấu hình bắt đầu bằng "jwt."
public class JwtProperties { // (đọc config trước)

    private String secret;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;
}
