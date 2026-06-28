package com.datn.engflow.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BlockResponse {
    private Long id;
    private String blockType;
    private String data;
    private Integer orderIndex;
}
