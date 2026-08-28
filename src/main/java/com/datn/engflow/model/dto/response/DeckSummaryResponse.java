package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
/**
 * class DeckSummaryResponse.
 */
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
