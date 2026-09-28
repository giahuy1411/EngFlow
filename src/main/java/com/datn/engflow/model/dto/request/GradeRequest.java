package com.datn.engflow.model.dto.request;

import lombok.Data;
import java.util.List;

/**
 * DTO chấm bài, bind từ body của {@code POST /api/lessons/{lessonId}/exercises/grade} và
 * {@code /submit}; cả hai đường đều chạy {@code ExerciseService.gradeExercises}.
 *
 * <p>{@link #answers} để null là hợp lệ và cho điểm 0/0 — đó là đường chấm tự động bằng
 * đáp án đúng, còn khi có danh sách thì service tự loại các mục không hợp lệ
 * (thiếu {@code exerciseId}, không tìm thấy bài, hoặc bài không có đáp án) ra khỏi mẫu số.
 */
@Data
public class GradeRequest {
    private List<AnswerItem> answers;

    /**
     * Một câu trả lời: bài tập nào, và người học trả lời gì.
     */
    @Data
    public static class AnswerItem {
        private Long exerciseId;
        private String userAnswer;
    }
}
