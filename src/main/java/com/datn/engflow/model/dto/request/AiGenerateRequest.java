package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
/**
 * class AiGenerateRequest.
 */
public class AiGenerateRequest {
    @NotNull(message = "lessonId không được để trống")
    private Long lessonId;

    private String exerciseType; // MULTIPLE_CHOICE, FILL_BLANK, MATCHING, TRANSLATION, LISTENING, or null for all

    @Min(value = 1, message = "count tối thiểu 1")
    @Max(value = 20, message = "count tối đa 20")
    private Integer count = 5;
}
