package com.datn.engflow.model.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * class LessonResponse.
 */
public class LessonResponse {
    private Long id;
    private String title;
    private String description;
    private String content;
    private String level;
    private String category;
    private Integer durationMinutes;
    private String thumbnailUrl;
    private String audioUrl;
    private Integer orderIndex;
    
    private Boolean isCompleted;
    private BigDecimal completionPercentage;

    private List<VocabularyDTO> vocabularies;
}
