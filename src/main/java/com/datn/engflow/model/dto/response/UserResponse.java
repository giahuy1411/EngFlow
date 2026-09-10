package com.datn.engflow.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * class UserResponse.
 */
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private Boolean isAdmin;
    private String currentLevel;
    private Integer totalPoints;
    private Integer currentStreak;
    private String lastLoginAt;
    private Boolean isPremium;
    private String premiumExpiry;
    /**
     * Lượt sinh AI đã dùng trong ngày. Trước đây là bộ đếm vĩnh viễn; từ khi quota
     * tính theo ngày thì nó chỉ đếm lượt của ngày hiện tại. Giữ nguyên tên field
     * để tương thích client cũ.
     */
    private Integer aiGenerationCount;
    /** true nếu admin hoặc premium còn hạn — client dùng flag này thay vì tự suy ra. */
    private Boolean hasPremiumAccess;
    /** Lượt sinh AI còn lại hôm nay; null nghĩa là không giới hạn. */
    private Integer aiGenerationsRemainingToday;
    private String token;
}
