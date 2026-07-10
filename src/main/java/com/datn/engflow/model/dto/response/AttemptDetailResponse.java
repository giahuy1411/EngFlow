package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AttemptDetailResponse {
    private Long id;
    private int score;
    private int total;
    private BigDecimal percentage;
    private LocalDateTime completedAt;
    private List<AttemptDetailItem> details;

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
