package com.datn.engflow.model.dto.request;

import com.datn.engflow.model.entity.SpeakingPromptMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * class CreateSpeakingPromptRequest.
 */
public class CreateSpeakingPromptRequest {
    @NotBlank
    private String title;
    private String description;
    @NotBlank
    private String prompt;
    private String level;
    private String category;
    private Boolean isPremium;
    private String thumbnailUrl;
    private Integer orderIndex;
    private Boolean isPublished;
    private Long lessonId;
    private SpeakingPromptMode mode;
    private String referenceText;
    private String referenceMediaObjectKey;
    private String referenceMediaUrl;
    @Min(15)
    @Max(600)
    private Integer maxDurationSeconds;
    @Min(1)
    @Max(100)
    private Integer attemptLimit;
}
