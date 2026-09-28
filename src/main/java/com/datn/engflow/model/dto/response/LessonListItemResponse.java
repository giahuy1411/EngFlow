package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.model.enums.SkillType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Một dòng trong danh sách bài học đã publish.
 *
 * <p>Tầng response của {@code GET /api/lessons}. Cố tình không mang cột
 * {@code content} (NVARCHAR(MAX)) — {@code LessonService} đọc qua projection nhẹ
 * cho danh sách; bản đầy đủ nằm ở {@link LessonResponse}.
 */
@Data
@Builder
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
