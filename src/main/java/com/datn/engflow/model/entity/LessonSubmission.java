package com.datn.engflow.model.entity;

import com.datn.engflow.model.enums.SkillType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * class LessonSubmission.
 */
public class LessonSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false, length = 50)
    private SkillType skillType;

    @Column(name = "submission_text", columnDefinition = "NVARCHAR(MAX)")
    private String submissionText;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "score")
    private Double score;

    @Column(name = "feedback", columnDefinition = "NVARCHAR(MAX)")
    private String feedback;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, GRADED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
