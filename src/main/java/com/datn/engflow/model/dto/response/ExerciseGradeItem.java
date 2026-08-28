package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/**
 * class ExerciseGradeItem.
 */
public class ExerciseGradeItem {
    private Long exerciseId;
    private boolean correct;
    private String userAnswer;
    private String correctAnswer;
}
