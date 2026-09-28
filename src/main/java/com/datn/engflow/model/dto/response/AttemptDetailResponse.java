package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chi tiết một lần làm bài: điểm tổng và từng câu đã trả lời.
 *
 * <p>Tầng response của {@code GET /api/lessons/{lessonId}/exercises/attempts/{attemptId}};
 * {@code ExerciseService.getAttemptDetail} dựng DTO này bằng cách parse thủ công
 * cột JSON {@code details} của {@link com.datn.engflow.model.entity.ExerciseAttempt},
 * không dùng mapper.
 */
@Data
@Builder
public class AttemptDetailResponse {
    private Long id;
    private int score;
    private int total;
    private BigDecimal percentage;
    private LocalDateTime completedAt;
    private List<AttemptDetailItem> details;

    /**
     * Một câu trong phần chi tiết lần làm bài.
     *
     * @param exerciseId id câu hỏi; null nếu trường không đọc được từ JSON
     * @param question nội dung câu hỏi tại thời điểm làm bài
     * @param userAnswer câu trả lời của người học
     * @param correctAnswer đáp án chuẩn
     * @param isCorrect kết luận chấm điểm của lần làm bài này
     * @param explanation giải thích kèm theo câu, null nếu câu không có
     */
    @Data
    @Builder
    public static class AttemptDetailItem {
        private Long exerciseId;
        private String question;
        private String userAnswer;
        private String correctAnswer;
        private boolean isCorrect;
        private String explanation;
    }
}
