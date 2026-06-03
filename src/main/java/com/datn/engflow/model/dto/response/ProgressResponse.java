package com.datn.engflow.model.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressResponse {
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer totalPoints;
    private List<AchievementDTO> unlockedAchievements;
}
