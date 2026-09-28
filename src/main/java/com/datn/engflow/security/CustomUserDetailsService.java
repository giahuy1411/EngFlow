package com.datn.engflow.security;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Cầu nối giữa bảng user trong DB và Spring Security: nạp {@link UserPrincipal} theo email.
 *
 * <p>Đây là mắt xích nằm giữa {@code JwtAuthenticationFilter} và DB. Mỗi request có Bearer token
 * hợp lệ đều gọi {@link #loadUserByUsername(String)} để nạp LẠI hàng user từ DB (JWT stateless,
 * không cache {@code SecurityContext}) — nhờ đó việc đổi quyền hay vô hiệu hoá tài khoản có hiệu
 * lực ngay ở request kế tiếp thay vì phải chờ token hết hạn.</p>
 *
 * <p>Không dùng cho form login: Spring Security gọi class này qua interface {@link UserDetailsService},
 * nhưng EngFlow đăng nhập bằng JWT nên {@code DaoAuthenticationProvider} không chạy và
 * {@code isEnabled()} do chính filter tự kiểm (xem {@code JwtAuthenticationFilter}).</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Nạp user theo email — EngFlow định danh bằng email, không có username riêng.
     *
     * @param email email lấy từ claim của JWT
     * @return principal bọc entity {@link User} kèm authorities ({@code ROLE_ADMIN}/{@code ROLE_USER})
     * @throws UsernameNotFoundException khi không có hàng user khớp email; filter bắt exception này
     *                                 ở nhánh catch-all rồi cho request đi tiếp KHÔNG có principal,
     *                                 để các endpoint {@code permitAll} vẫn phục vụ được.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new UserPrincipal(user);
    }
}
