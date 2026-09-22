package com.datn.engflow.model.entity;

import com.datn.engflow.model.enums.LessonLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * class User.
 */
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    @JsonIgnore
    private String passwordHash;

    @org.hibernate.annotations.Nationalized
    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "avatar_url", length = 300)
    private String avatarUrl;

    @Builder.Default
    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_level", length = 20)
    private LessonLevel currentLevel;

    @Builder.Default
    @Column(name = "total_points")
    private Integer totalPoints = 0;


    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_premium")
    private Boolean isPremium = false;

    @Column(name = "premium_expiry")
    private LocalDate premiumExpiry;

    // audit-v13 F-13-08: `last_study_date` and `current_streak` were REMOVED here.
    // Both were legacy counters from the old "streak = consecutive login days" model.
    // The streak refactor moved the source of truth to the `study_days` table
    // (entity StudyDay) and nothing has written or read these columns since:
    //   - 0 writers in src/main (no @PrePersist/@PreUpdate, no trigger)
    //   - 0 readers in src/main (every streak value served to clients comes from
    //     StreakService -> StudyActivityService -> study_days)
    // The DB columns were dropped in the same change (ddl-auto=update never drops,
    // so the ALTER was run by hand). Do not reintroduce them.

    /**
     * Số lượt sinh từ AI đã dùng, tính trong ngày ghi ở {@link #aiQuotaDate}.
     * Trước đây cột này là bộ đếm vĩnh viễn; từ khi quota tính theo ngày thì nó
     * chỉ còn nghĩa "đã dùng bao nhiêu lượt của ngày {@code aiQuotaDate}".
     */
    @Builder.Default
    @Column(name = "ai_generation_count")
    private Integer aiGenerationCount = 0;

    /** Ngày mà {@link #aiGenerationCount} đang đếm. Null nghĩa là chưa dùng lượt nào. */
    @Column(name = "ai_quota_date")
    private LocalDate aiQuotaDate;
}
