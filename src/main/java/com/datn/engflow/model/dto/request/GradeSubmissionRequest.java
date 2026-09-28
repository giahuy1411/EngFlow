package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO chấm điểm bài nộp bài học, chấm nhận điểm số nguyên 0-10 kèm nhận xét tuỳ chọn.
 *
 * <p>Hiện KHÔNG có endpoint nào bind lớp này. Chấm bài nói đi qua
 * {@link GradeSpeakingSubmissionRequest} ở {@code SpeakingSubmissionController}, còn chấm
 * bài học đi qua {@code LessonSubmissionController}. Lớp được giữ lại như một trạng thái chờ
 * — khác {@link GradeSpeakingSubmissionRequest} ở chỗ cho phép điểm lẻ và không bắt buộc
 * nhận xét.
 */
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
