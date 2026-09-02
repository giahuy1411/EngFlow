package com.datn.engflow.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * class ExerciseGradeItem.
 */
public class ExerciseGradeItem {
    private Long exerciseId;
    private boolean correct;
    private String userAnswer;
    private String correctAnswer;
    /**
     * F7-BUG02: set when the server cannot grade the answer (e.g. malformed key block).
     * Frontend should display the question as "Không thể chấm — xem đáp án" and reveal correctAnswer.
     */
    private boolean ungradeable;
}
