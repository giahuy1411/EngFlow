package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests cho {@link UserService#hasUnlimitedAiGeneration(User)}.
 *
 * <p>Commit 749a8f5 từng làm mất nhánh {@code isAdmin}, khiến admin bị chặn
 * quota 5 lần sinh từ vựng AI dù comment ở {@code AiVocabController} ghi rõ
 * "premium/admin unlimited". Test này khóa lại contract đó.
 *
 * <p>Constructor được gọi trực tiếp với dependency {@code null} vì method
 * thuần túy đọc field của entity, không chạm repository/Redis.
 */
class UserServiceUnlimitedAiGenerationTest {

    private final UserService userService =
            new UserService(null, null, null, null, null, null, null);

    @Test
    @DisplayName("Admin luôn unlimited, kể cả không premium và không có expiry")
    void adminWithoutPremium_isUnlimited() {
        User admin = User.builder()
                .isAdmin(true)
                .isPremium(false)
                .build();

        assertThat(userService.hasUnlimitedAiGeneration(admin)).isTrue();
    }

    @Test
    @DisplayName("Admin vẫn unlimited khi premium đã hết hạn")
    void adminWithExpiredPremium_isUnlimited() {
        User admin = User.builder()
                .isAdmin(true)
                .isPremium(true)
                .premiumExpiry(LocalDate.now().minusDays(30))
                .build();

        assertThat(userService.hasUnlimitedAiGeneration(admin)).isTrue();
    }

    @Test
    @DisplayName("Premium còn hạn thì unlimited")
    void premiumNotExpired_isUnlimited() {
        User premium = User.builder()
                .isAdmin(false)
                .isPremium(true)
                .premiumExpiry(LocalDate.now().plusDays(10))
                .build();

        assertThat(userService.hasUnlimitedAiGeneration(premium)).isTrue();
    }

    @Test
    @DisplayName("Premium không có expiry (vĩnh viễn) thì unlimited")
    void premiumWithoutExpiry_isUnlimited() {
        User premium = User.builder()
                .isAdmin(false)
                .isPremium(true)
                .premiumExpiry(null)
                .build();

        assertThat(userService.hasUnlimitedAiGeneration(premium)).isTrue();
    }

    @Test
    @DisplayName("Premium hết hạn ngày hôm qua thì bị giới hạn")
    void premiumExpiredYesterday_isLimited() {
        User premium = User.builder()
                .isAdmin(false)
                .isPremium(true)
                .premiumExpiry(LocalDate.now().minusDays(1))
                .build();

        assertThat(userService.hasUnlimitedAiGeneration(premium)).isFalse();
    }

    @Test
    @DisplayName("Premium hết hạn đúng hôm nay vẫn còn hiệu lực")
    void premiumExpiringToday_isUnlimited() {
        User premium = User.builder()
                .isAdmin(false)
                .isPremium(true)
                .premiumExpiry(LocalDate.now())
                .build();

        assertThat(userService.hasUnlimitedAiGeneration(premium)).isTrue();
    }

    @Test
    @DisplayName("User thường không premium thì bị giới hạn")
    void freeUser_isLimited() {
        User freeUser = User.builder()
                .isAdmin(false)
                .isPremium(false)
                .build();

        assertThat(userService.hasUnlimitedAiGeneration(freeUser)).isFalse();
    }

    @Test
    @DisplayName("Null isAdmin/isPremium không làm nổ, coi như không unlimited")
    void nullFlags_areTreatedAsFalse() {
        User user = User.builder().build();

        assertThat(userService.hasUnlimitedAiGeneration(user)).isFalse();
    }
}
