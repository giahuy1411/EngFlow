package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.AvatarRequest;
import com.datn.engflow.model.dto.request.LoginRequest;
import com.datn.engflow.model.dto.request.RegisterRequest;
import com.datn.engflow.model.dto.response.UserResponse;
import com.datn.engflow.service.CloudinaryService;
import com.datn.engflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Cổng REST cho vòng đời tài khoản: đăng ký, đăng nhập, quên/đặt lại mật khẩu, lấy hồ sơ
 * và cập nhật ảnh đại diện.
 *
 * <p>Tầng controller — mọi việc thật nằm ở {@link UserService}; đẩy ảnh lên Cloudinary
 * thì gọi {@link CloudinaryService}. Các endpoint trả về {@link UserResponse} làm token
 * cho frontend (không có endpoint refresh token — hết hạn thì đăng nhập lại).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    /**
     * Đăng ký tài khoản mới và đăng nhập luôn.
     *
     * @param registerRequest dữ liệu đăng ký đã qua Bean Validation
     * @return hồ sơ người dùng kèm JWT, HTTP 201
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        UserResponse response = userService.register(registerRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Đăng nhập bằng email + mật khẩu.
     *
     * @param loginRequest thông tin đăng nhập đã qua Bean Validation
     * @return hồ sơ người dùng kèm JWT
     */
    @PostMapping("/login")
    public ResponseEntity<UserResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        UserResponse response = userService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Yêu cầu mã OTP đặt lại mật khẩu.
     *
     * @param request email cần gửi OTP
     * @return thông báo trả về, giống nhau dù email có tồn tại hay không
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody com.datn.engflow.model.dto.request.ForgotPasswordRequest request) {
        String message = userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * Đặt mật khẩu mới bằng OTP đã nhận.
     *
     * @param request email, OTP và mật khẩu mới
     * @return thông báo đặt lại thành công
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody com.datn.engflow.model.dto.request.ResetPasswordRequest request) {
        userService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "\u0110\u1eb7t l\u1ea1i m\u1eadt kh\u1ea9u th\u00e0nh c\u00f4ng. B\u1ea1n c\u00f3 th\u1ec3 \u0111\u0103ng nh\u1eadp v\u1edbi m\u1eadt kh\u1ea9u m\u1edbi."));
    }

    /**
     * Hồ sơ của chính người đang đăng nhập.
     *
     * @param authentication principal hiện tại
     * @return hồ sơ người dùng, hoặc 401 rỗng nếu chưa đăng nhập
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserResponse response = userService.getProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Đổi mật khẩu khi đã đăng nhập (không cần OTP).
     *
     * @param request       mật khẩu cũ và mật khẩu mới
     * @param authentication principal hiện tại
     * @return thông báo đổi mật khẩu thành công, hoặc 401 rỗng nếu chưa đăng nhập
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody com.datn.engflow.model.dto.request.ChangePasswordRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(java.util.Map.of("message", "Đổi mật khẩu thành công"));
    }

    /**
     * Gán ảnh đại diện bằng URL bên ngoài.
     *
     * @param request        URL ảnh mới
     * @param authentication principal hiện tại
     * @return hồ sơ sau khi cập nhật ảnh, hoặc 401 rỗng nếu chưa đăng nhập
     */
    @PutMapping("/avatar")
    public ResponseEntity<UserResponse> updateAvatar(
            @Valid @RequestBody AvatarRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserResponse response = userService.updateAvatar(authentication.getName(), request.getAvatarUrl());
        return ResponseEntity.ok(response);
    }

    /**
     * Tải ảnh đại diện lên Cloudinary rồi gắn URL vào hồ sơ.
     *
     * @param file           tệp ảnh do client gửi lên
     * @param authentication principal hiện tại
     * @return hồ sơ sau khi cập nhật ảnh
     */
    @PostMapping("/avatar/upload")
    public ResponseEntity<UserResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            String avatarUrl = cloudinaryService.uploadAvatar(file);
            UserResponse response = userService.updateAvatar(authentication.getName(), avatarUrl);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // CloudinaryService ném loại này cho input không hợp lệ (file rỗng, không phải
            // ảnh) → 400 là đúng. Ghi log để lỗi từ phía client không bị im lặng.
            log.warn("Avatar upload rejected for {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // Lỗi hạ tầng (Cloudinary/DB): vẫn trả 500 rỗng để giữ nguyên API shape, nhưng
            // ghi log kèm stack để vận hành chẩn đoán được — trước đây nuốt lỗi hoàn toàn.
            log.error("Avatar upload failed for {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
