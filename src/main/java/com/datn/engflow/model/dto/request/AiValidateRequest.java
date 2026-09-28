package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * DTO kiểm tra bài tập trước khi lưu, bind từ body của
 * {@code POST /api/admin/exercises/ai/validate}.
 *
 * <p>Mỗi {@link ExerciseDraft} được {@code AdminAiExerciseController} dựng tạm thành entity
 * {@code Exercise} rồi chạy qua {@code AiExerciseService.validateSchema}; nếu
 * {@link #useAiReview} bật thì các bài đã hợp lệ còn được đưa qua
 * {@code AiExerciseService.reviewExercise} — gọi mô hình lần nữa nên chỉ bật khi admin cần
 * chấm chất lượng, không phải kiểm tra hình thức.
 */
@Data
public class AiValidateRequest {
    @NotNull(message = "exercises không được để trống")
    private List<ExerciseDraft> exercises;

    private boolean useAiReview = false;

    /**
     * Một bài tập sơ bộ do AI sinh, chỉ mang những trường cần kiểm tra.
     */
    @Data
    public static class ExerciseDraft {
        private String question;
        private String options; // JSON array string
        private String correctAnswer;
        private String exerciseType;
        private String explanation;
        private String audioUrl;
    }
}
