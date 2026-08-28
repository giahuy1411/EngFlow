package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * record GradeSpeakingSubmissionRequest.
 */
public record GradeSpeakingSubmissionRequest(
        @NotNull(message = "Điểm không được để trống")
        @DecimalMin(value = "0.0", message = "Điểm phải nằm trong khoảng từ 0 đến 10")
        @DecimalMax(value = "10.0", message = "Điểm phải nằm trong khoảng từ 0 đến 10")
        @Digits(integer = 2, fraction = 1, message = "Điểm chỉ được có tối đa một chữ số thập phân")
        BigDecimal score,

        @NotBlank(message = "Nhận xét không được để trống")
        @Size(max = 4000, message = "Nhận xét không được vượt quá 4000 ký tự")
        String feedback,

        @Size(max = 4000, message = "Ghi chú nội bộ không được vượt quá 4000 ký tự")
        String privateNote
) {
}
