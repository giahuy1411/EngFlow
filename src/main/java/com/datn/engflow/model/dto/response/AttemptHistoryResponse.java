package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AttemptHistoryResponse {
    private Long id;
    private int score;
    private int total;
    private BigDecimal percentage;
    private LocalDateTime completedAt;
}
