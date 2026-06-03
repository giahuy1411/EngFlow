package com.datn.engflow.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryDTO {
    private Integer rank;
    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private Integer totalPoints;
    private Integer currentStreak;
    private String currentLevel;
}
