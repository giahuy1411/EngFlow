package com.datn.engflow.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
/**
 * class SnapshotResponse.
 */
public class SnapshotResponse {
    private Long id;
    private LocalDateTime createdAt;
}
