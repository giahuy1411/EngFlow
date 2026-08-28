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
    private Integer aiGenerationCount;
    private String token;
}
