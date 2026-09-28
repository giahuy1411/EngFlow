package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Tóm tắt một bộ thẻ từ vựng cho màn hình danh sách deck.
 *
 * <p>Tầng response của {@code /api/decks} (deck public và deck của người dùng).
 * {@code DeckService.toSummary} dựng DTO với {@code wordCount} để null, rồi
 * {@code attachWordCounts} gắn số từ bằng một query gộp theo danh sách id.
 */
@Data
@Builder
public class DeckSummaryResponse {
    private Long id;
    private String name;
    private String description;
    private String source;
    private String cefrLevel;
    private Boolean isPublic;
    private String thumbnailUrl;
    private Integer wordCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
