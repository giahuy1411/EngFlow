package com.datn.engflow.model.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer totalPoints;
    private Integer currentStreak;
    private Integer totalVocabulary;
    private Integer totalExercises;
    private Integer correctExercises;
    private List<DailyPointEntry> dailyPoints;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyPointEntry {
        private String date;
        private Integer points;
    }
}
