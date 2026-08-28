package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_prompts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * class SpeakingPrompt.
 */
public class SpeakingPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 200)
    private String title;

    @org.hibernate.annotations.Nationalized
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String prompt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SpeakingPromptMode mode = SpeakingPromptMode.FREE_SPEAKING;

    @org.hibernate.annotations.Nationalized
    @Column(name = "reference_text", columnDefinition = "NVARCHAR(MAX)")
    private String referenceText;

    @Column(name = "reference_media_object_key", length = 500)
    private String referenceMediaObjectKey;

    @Column(name = "reference_media_url", length = 1000)
    private String referenceMediaUrl;

    @Builder.Default
    @Column(name = "max_duration_seconds")
    private Integer maxDurationSeconds = 120;

    @Builder.Default
    @Column(name = "attempt_limit")
    private Integer attemptLimit = 10;

    @Column(length = 20)
    private String level;

    @Column(length = 50)
    private String category;

    @Builder.Default
    @Column(name = "is_premium")
    private Boolean isPremium = false;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Builder.Default
    @Column(name = "is_published")
    private Boolean isPublished = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
