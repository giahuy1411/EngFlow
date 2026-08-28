package com.datn.engflow.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
/**
 * class SectionResponse.
 */
public class SectionResponse {
    private Long id;
    private String title;
    private Integer orderIndex;
    private List<BlockResponse> blocks;
}
