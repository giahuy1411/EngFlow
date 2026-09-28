package com.datn.engflow.model.entity;

import com.datn.engflow.model.enums.LessonLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Bài học dạng video: video YouTube kèm transcript chia theo từng dòng để luyện
 * shadowing và chấm điểm đọc theo.
 *
 * <p>Tầng entity. {@link VideoLessonService} quản lý nội dung và parse
 * {@link #transcriptJson} thành danh sách dòng cho màn luyện tập; danh sách dòng đó
 * cũng là đáp án mà {@link ShadowingAiGradingService} dùng để chấm
 * {@link VideoAttempt}. Cột transcript là {@code NVARCHAR(MAX)} vì mỗi bài có thể
 * chứa cả bài hát/đoạn hội thoại dài.
 */
@Entity
@Table(name = "video_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 200)
    private String title;

    @org.hibernate.annotations.Nationalized
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "youtube_video_id", nullable = false, length = 20)
    private String youtubeVideoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LessonLevel level;

    @org.hibernate.annotations.Nationalized
    @Column(length = 50)
    private String category;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @org.hibernate.annotations.Nationalized
    @Column(name = "transcript_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String transcriptJson;

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
