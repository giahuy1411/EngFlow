package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SectionRequest {
    @NotBlank
    private String title;
    private Integer orderIndex;
}
