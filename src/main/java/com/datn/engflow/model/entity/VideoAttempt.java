package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "video_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * class VideoAttempt.
 */
public class VideoAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_lesson_id", nullable = false)
    private VideoLesson videoLesson;

    @Column(name = "line_index", nullable = false)
    private Integer lineIndex;

    @Column(name = "media_object_key", length = 500)
    private String mediaObjectKey;

    @Column(name = "media_type", length = 100)
    private String mediaType;

    @Builder.Default
    @Column(length = 30)
    private String status = "SUBMITTED";

    @Column(precision = 3, scale = 1)
    private BigDecimal score;

    @org.hibernate.annotations.Nationalized
    @Column(name = "admin_feedback", columnDefinition = "NVARCHAR(MAX)")
    private String adminFeedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
}
