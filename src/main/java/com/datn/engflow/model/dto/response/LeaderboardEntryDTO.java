package com.datn.engflow.model.dto.response;

import lombok.*;

/**
 * Một dòng của bảng xếp hạng.
 *
 * <p>Tầng response của {@code GET /api/leaderboard}; {@code LeaderboardService}
 * map từ entity {@code User} cộng thêm hạng tính theo vị trí trong trang và streak
 * đọc gộp cho cả trang. Cùng một DTO phục vụ cả hai dạng: phân trang và lấy N hạng
 * đầu.
 */
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
