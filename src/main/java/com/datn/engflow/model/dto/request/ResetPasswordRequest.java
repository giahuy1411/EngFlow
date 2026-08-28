package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for reset password request with OTP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * class ResetPasswordRequest.
 */
public class ResetPasswordRequest {

    /**
     * The email address associated with the password reset.
     */
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    /**
     * The OTP code sent to the email.
     */
    @NotBlank(message = "Mã OTP không được để trống")
    private String otp;

    /**
     * The new password.
     */
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu phải chứa ít nhất 6 ký tự")
    private String newPassword;
}
