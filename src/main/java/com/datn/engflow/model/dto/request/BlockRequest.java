package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlockRequest {
    @NotBlank
    private String blockType;
    private String data;
    private Integer orderIndex;
}
