package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một dòng trong lịch sử làm bài của một bài học.
 *
 * <p>Tầng response của {@code GET /api/lessons/{lessonId}/exercises/attempts}:
 * {@code ExerciseService.getAttemptHistory} map thẳng từ entity
 * {@code ExerciseAttempt}, không kèm phần chi tiết từng câu — chi tiết nằm ở
 * {@link AttemptDetailResponse}.
 */
@Data
@Builder
public class AttemptHistoryResponse {
    private Long id;
    private int score;
    private int total;
    private BigDecimal percentage;
    private LocalDateTime completedAt;
}
