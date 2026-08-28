package com.datn.engflow.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * class ProgressResponse.
 */
public class ProgressResponse {
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer totalPoints;
}
