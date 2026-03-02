package vn.huy.digital_wallet.config;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.huy.digital_wallet.common.Status;
import vn.huy.digital_wallet.model.User;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Getter
public class UserDetailsImpl implements UserDetails { // Bọc User Entity

    private final User user;

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.user.getRole()));
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public @NonNull String getUsername() {
        return user.getUsername();
    }

    // Các cấu hình trạng thái tài khoản
    @Override
    public boolean isAccountNonExpired() {
        return true; // Tài khoản chưa hết hạn
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != Status.BANNED; // Check xem tài khoản có bị khóa không
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Mật khẩu chưa hết hạn
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == Status.ACTIVE; // Chỉ ACTIVE mới được dùng
    }

}
