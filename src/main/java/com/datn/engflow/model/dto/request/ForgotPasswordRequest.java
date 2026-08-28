package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for forgot password request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * class ForgotPasswordRequest.
 */
public class ForgotPasswordRequest {

    /**
     * The email address to send the OTP to.
     */
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
}
