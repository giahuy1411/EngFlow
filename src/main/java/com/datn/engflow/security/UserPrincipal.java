package com.datn.engflow.security;

import com.datn.engflow.model.entity.User;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
/**
 * class UserPrincipal.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final boolean active;
    private final Collection<SimpleGrantedAuthority> authorities;

    /**
     * Thực thể {@link User} mà principal được dựng từ đó. Được giữ lại vì
     * {@code CustomUserDetailsService} đã nạp nguyên hàng này từ DB ở mỗi request
     * (JWT stateless — không có cache SecurityContext), nên nơi cần đọc cột quyền
     * (ví dụ {@code UserService.hasPremiumAccess}) dùng trực tiếp thay vì gọi lại
     * repository. Không dùng nó để trả lời "gói còn hạn không" một cách trừu tượng:
     * hãy luôn chuyển cho {@code hasPremiumAccess} để phép thử {@code premiumExpiry}
     * so với {@code clock} vẫn là nguồn sự thật duy nhất.
     */
    private final User user;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.active = user.getIsActive();
        this.user = user;
        this.authorities = Collections.singletonList(
            new SimpleGrantedAuthority(Boolean.TRUE.equals(user.getIsAdmin()) ? "ROLE_ADMIN" : "ROLE_USER")
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
