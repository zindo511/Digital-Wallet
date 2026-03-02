package vn.huy.digital_wallet.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    // 1. Lấy username từ token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 2. Tạo access token
    public String generateToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, jwtProperties.getAccessTokenExpiration());
    }

    // 3. Tạo refresh token
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, jwtProperties.getRefreshTokenExpiration());
    }

    // Hàm build chung để tái sử dụng
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims) // Những thông tin phụ (nếu muốn nhét thêm vào token)
                .subject(userDetails.getUsername()) // Subject (chủ thể) thường là username hoặc email
                .issuedAt(new Date(System.currentTimeMillis())) // Thời điểm phát hành
                .expiration(new Date(System.currentTimeMillis() + expiration)) // Thời điểm hết hạn
                .signWith(getSignInKey()) // Ký bằng Secret Key
                .compact(); // Nén lại thành chuỗi String
    }

    // 4. Kiểm tra token có hợp lệ không
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        // Token hợp lệ nếu username khớp với database và token chưa hết hạn
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Hàm phụ trợ giải mã
    private  <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey()) // Đưa chìa khoá vào để mở token
                .build()
                .parseSignedClaims(token) // Bóc tách dữ liệu
                .getPayload(); // Lấy phần thân (payload) chứa thông tin
    }

    // Lấy Secret Key từ file cấu hình và chuyển đổi nó thành dạng Key của mã hóa HMAC
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
