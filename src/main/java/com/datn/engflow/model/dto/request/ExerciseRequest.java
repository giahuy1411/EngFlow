package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
