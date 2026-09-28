package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO tạo và sửa bài tập trong một bài học, bind từ body của {@code POST /api/admin/exercises}
 * và {@code PUT /api/admin/exercises/{id}}.
 *
 * <p>{@code ExerciseService.updateExercise} áp kiểu patch: chỉ trường khác null mới ghi đè,
 * nên cùng một lớp này vừa tạo mới vừa sửa. Kiểm tra {@code options} cũng chỉ chạy khi
 * request có gửi {@code options}, vì hàng loạt bài MULTIPLE_CHOICE cũ đang có {@code options}
 * bị null.
 */
@Data
public class ExerciseRequest {
    @NotNull
    private Long lessonId;

    @NotBlank
    private String question;

    private String options;

    @NotBlank
    private String correctAnswer;

    @NotBlank
    private String exerciseType;

    private String difficulty;

    private String explanation;

    private String imageUrl;

    private String audioUrl;

    private Integer orderIndex;
}
