package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.model.enums.SkillType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Lightweight lesson summary for admin lists — excludes heavy NVARCHAR(MAX)
 * content fields to keep list responses small and fast.
 */
@Data
@Builder
/**
 * class LessonSummaryDTO.
 */
public class LessonSummaryDTO {
    private Long id;
    private String title;
    private String description;
    private LessonLevel level;
    private String category;
    private Integer durationMinutes;
    private String thumbnailUrl;
    private SkillType skillType;
    private Integer orderIndex;
    private Boolean isPublished;
    private LocalDateTime createdAt;
}
