package com.datn.engflow.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsDTO {
    private long totalUsers;
    private long totalLessons;
    private long totalVocabulary;
    private long totalAchievements;
    private long activeUsers;
    private long recentUsers; // Users who studied in the last 7 days
}
