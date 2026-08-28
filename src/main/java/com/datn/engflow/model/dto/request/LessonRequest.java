package com.datn.engflow.model.dto.request;

import com.datn.engflow.model.enums.LessonLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * class LessonRequest.
 */
public class LessonRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    
    private String content;

    @NotNull(message = "Level is required")
    private LessonLevel level;

    private String category;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private String thumbnailUrl;

    private String audioUrl;

    private Integer orderIndex;

    private String skillType;
    
    private Boolean isPublished;
}
