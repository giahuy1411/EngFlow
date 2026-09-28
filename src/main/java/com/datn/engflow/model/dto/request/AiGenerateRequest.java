package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO yêu cầu sinh bài tập AI cho một bài học, bind từ body của ba endpoint
 * {@code POST /api/admin/exercises/ai/generate-async}, {@code /generate} và
 * {@code /generate-all}.
 *
 * <p>{@link #exerciseType} được {@code AdminAiExerciseController} chuyển qua
 * {@code ExerciseType.valueOf(type.toUpperCase())}, nên phải là tên hằng của
 * {@code ExerciseType}. Gửi null hoặc chuỗi rỗng thì controller giữ type là null và
 * {@code AiExerciseService} tự chọn loại.
 */
@Data
public class AiGenerateRequest {
    @NotNull(message = "lessonId không được để trống")
    private Long lessonId;

    private String exerciseType; // MULTIPLE_CHOICE, FILL_BLANK, MATCHING, TRANSLATION, LISTENING, or null for all

    @Min(value = 1, message = "count tối thiểu 1")
    @Max(value = 20, message = "count tối đa 20")
    private Integer count = 5;
}
