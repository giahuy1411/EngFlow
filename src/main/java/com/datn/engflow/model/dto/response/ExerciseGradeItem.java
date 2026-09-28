package com.datn.engflow.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Kết quả chấm điểm của một câu hỏi trong lần nộp bài.
 *
 * <p>Tầng response của {@code POST /api/lessons/{lessonId}/exercises/grade} và
 * {@code /submit} ({@link GradeResponse}); {@code ExerciseService.gradeExercises}
 * sinh ra từng item rồi gom vào {@code results}. Câu không chấm được vẫn có mặt
 * trong danh sách nhưng không tính vào mẫu số — xem {@link #isUngradeable()}.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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
