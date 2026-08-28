package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.model.enums.SkillType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
/**
 * class LessonListItemResponse.
 */
public class LessonListItemResponse {
    private Long id;
    private String title;
    private String description;
    private LessonLevel level;
    private String category;
    private Integer durationMinutes;
    private String thumbnailUrl;
    private String audioUrl;
    private SkillType skillType;
    private Integer orderIndex;
    private Boolean isCompleted;
    private BigDecimal completionPercentage;
}
