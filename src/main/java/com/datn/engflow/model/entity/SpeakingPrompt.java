package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Đề bài luyện nói: câu tự do hoặc đoạn mẫu để đọc theo, kèm giới hạn thời lượng
 * và số lượt.
 *
 * <p>Tầng entity. {@link SpeakingPromptService} quản lý vòng đời đề (tạo/sửa/xoá,
 * chặn xoá khi đã có bài nộp) và là nơi kiểm tra {@link #mode} phải có
 * {@link #referenceText}; {@link SpeakingSubmissionService} đọc đề để chấm.
 * {@link #attemptLimit} hiện chỉ được admin ghi và trả ra DTO — chưa có service nào
 * đếm số lượt để chặn, nên đừng hiểu nó là hạn chế đang hoạt động.
 */
@Entity
@Table(name = "speaking_prompts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
