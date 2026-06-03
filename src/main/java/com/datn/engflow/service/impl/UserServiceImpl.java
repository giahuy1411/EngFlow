package com.datn.engflow.service.impl;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.LoginRequest;
import com.datn.engflow.model.dto.request.RegisterRequest;
import com.datn.engflow.model.dto.response.UserResponse;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.model.enums.UserRole;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.JwtTokenProvider;
import com.datn.engflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Bắt đầu đăng ký user mới: username={}, email={}", request.getUsername(), request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.error("Đăng ký thất bại: Email {} đã tồn tại", request.getEmail());
            throw new BadRequestException("Email đã tồn tại trên hệ thống");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            log.error("Đăng ký thất bại: Username {} đã tồn tại", request.getUsername());
            throw new BadRequestException("Tên đăng nhập đã tồn tại");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(UserRole.USER)
                .currentLevel(LessonLevel.BEGINNER)
                .avatarUrl("https://api.dicebear.com/7.x/adventurer/svg?seed=" + request.getUsername())
                .totalPoints(0)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Đăng ký thành công user: id={}, tự động tạo token đăng nhập", savedUser.getId());

        String jwt = tokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole().name());
        return mapToUserResponse(savedUser, jwt);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        log.info("Bắt đầu đăng nhập cho email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        String jwt = tokenProvider.generateToken(user.getEmail(), user.getRole().name());

        log.info("Đăng nhập thành công cho user: id={}, role={}", user.getId(), user.getRole());
        return mapToUserResponse(user, jwt);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        log.info("Lấy thông tin profile cho email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa");
        }
        return mapToUserResponse(user, null);
    }

    @Override
    @Transactional
    public void changePassword(String email, com.datn.engflow.model.dto.request.ChangePasswordRequest request) {
        log.info("Thay đổi mật khẩu cho email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Thay đổi mật khẩu thành công cho email: {}", email);
    }

    private UserResponse mapToUserResponse(User user, String token) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .currentLevel(user.getCurrentLevel().name())
                .totalPoints(user.getTotalPoints())
                .currentStreak(user.getCurrentStreak() != null ? user.getCurrentStreak() : 0)
                .lastStudyDate(user.getLastStudyDate() != null ? user.getLastStudyDate().toString() : null)
                .token(token)
                .build();
    }
}
