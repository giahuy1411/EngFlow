package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExerciseRequest {
    @NotNull(message = "ID bài học không được để trống")
    private Long lessonId;

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Câu hỏi không được để trống")
    private String question;

    private String options; // JSON string

    @NotBlank(message = "Đáp án đúng không được để trống")
    private String correctAnswer;

    private String explanation;

    @NotBlank(message = "Loại bài tập không được để trống")
    private String exerciseType; // multiple_choice, fill_blank

    private String difficulty;

    private Integer points;

    private String audioUrl;

    private String imageUrl;
}
