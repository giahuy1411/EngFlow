package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeSubmissionRequest {

    @NotNull(message = "Điểm số không được để trống")
    @Min(value = 0, message = "Điểm số phải lớn hơn hoặc bằng 0")
    @Max(value = 10, message = "Điểm số phải nhỏ hơn hoặc bằng 10")
    private Double score;

    private String feedback;
}
