package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
/**
 * class AiValidateRequest.
 */
public class AiValidateRequest {
    @NotNull(message = "exercises không được để trống")
    private List<ExerciseDraft> exercises;

    private boolean useAiReview = false;

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
