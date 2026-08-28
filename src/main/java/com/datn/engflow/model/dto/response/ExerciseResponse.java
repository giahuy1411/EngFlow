package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * class ExerciseResponse.
 */
public class ExerciseResponse {
    private Long id;
    private Long lessonId;
    private String lessonTitle;
    private String question;
    private String options;
    private String correctAnswer;
    private String exerciseType;
    private String difficulty;
    private String explanation;
    private String imageUrl;
    private String audioUrl;
    private Integer orderIndex;
}
