package com.datn.engflow.security;

import com.datn.engflow.model.entity.User;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Cầu nối giữa entity {@link User} và Spring Security: đây là lớp hiện thực
 * {@link UserDetails} mà {@code CustomUserDetailsService} trả về sau khi xác thực JWT.
 *
 * <p>Principal mang theo: {@code email} (được dùng làm tên đăng nhập vì EngFlow định danh
 * bằng email, không có username riêng), {@code password} (hash BCrypt đã lưu), và
 * {@code authorities} — đúng một quyền {@code ROLE_ADMIN} hoặc {@code ROLE_USER} suy ra từ
 * cờ {@code isAdmin} của entity. Cờ {@code active} của tài khoản được ánh xạ vào
 * {@link #isEnabled()}, nên tài khoản bị vô hiệu hoá sẽ bị Spring Security chặn ngay ở tầng filter.</p>
 *
 * <p>Lưu ý quan trọng: <b>không có</b> trường {@code isPremium} ở đây. Premium phụ thuộc thời hạn
 * ({@code premiumExpiry}) nên không thể chốt cứng lúc dựng principal; muốn biết người dùng còn
 * Premium hay không hãy gọi {@code UserService.hasPremiumAccess(...)}. Entity {@link User} gốc vẫn
 * được giữ lại trong trường {@link #user} để phục vụ phép kiểm đó mà không phải truy vấn lại DB.</p>
 */
@Getter
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
        this.active = Boolean.TRUE.equals(user.getIsActive());
        this.user = user;
        this.authorities = Collections.singletonList(
            new SimpleGrantedAuthority(Boolean.TRUE.equals(user.getIsAdmin()) ? "ROLE_ADMIN" : "ROLE_USER")
        );
    }

    /**
     * "Tên đăng nhập" theo nghĩa Spring Security. EngFlow dùng email làm định danh duy nhất,
     * nên đây chính là {@code email} chứ không phải một username riêng — nhờ vậy mọi chỗ đọc
     * {@code authentication.getName()} (ví dụ FlashcardService, SpeakingService) đều nhận email.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Tài khoản không bao giờ hết hạn theo thời gian — EngFlow không mô hình hoá hạn tài khoản.
     * Luôn trả {@code true} để Spring Security không chặn vì lý do này.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Tài khoản không bao giờ bị khoá (không có cơ chế lock sau nhiều lần đăng nhập sai).
     * Luôn trả {@code true}; việc vô hiệu hoá người dùng đi qua {@link #isEnabled()} bằng cờ
     * {@code isActive} thay vì cơ chế lock của Spring.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Thông tin xác thực (mật khẩu) không hết hạn — không có chính sách buộc đổi mật khẩu định kỳ.
     * Luôn trả {@code true} để không chặn đăng nhập.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Tài khoản có đang được bật hay không, lấy trực tiếp từ cờ {@code isActive} của entity
     * {@link User} (đã chuẩn hoá null → {@code false} lúc dựng principal). Spring Security gọi
     * hàm này trong quá trình xác thực: trả {@code false} là tài khoản bị từ chối đăng nhập.
     */
    @Override
    public boolean isEnabled() {
        return active;
    }
}
