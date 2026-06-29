package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.entity.Vocabulary;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
