package com.datn.engflow.model.entity;

import com.datn.engflow.model.enums.LessonLevel;
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

    @Column(name = "last_study_date")
    private LocalDate lastStudyDate;

    @Builder.Default
    @Column(name = "current_streak")
    private Integer currentStreak = 0;
}
