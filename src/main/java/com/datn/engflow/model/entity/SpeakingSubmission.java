package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * class SpeakingSubmission.
 */
public class SpeakingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false)
    private SpeakingPrompt prompt;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "media_object_key", length = 500)
    private String mediaObjectKey;

    @Column(name = "media_type", length = 100)
    private String mediaType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SpeakingSubmissionStatus status = SpeakingSubmissionStatus.SUBMITTED;

    @Column(name = "assessment_provider", length = 100)
    private String assessmentProvider;

    @Column(name = "assessment_error", length = 500)
    private String assessmentError;

    @Column(name = "pronunciation_accuracy")
    private Double pronunciationAccuracy;

    @Column(name = "pronunciation_fluency")
    private Double pronunciationFluency;

    @Column(name = "pronunciation_completeness")
    private Double pronunciationCompleteness;

    @Column(name = "pronunciation_prosody")
    private Double pronunciationProsody;

    @Column(name = "pronunciation_details_json", columnDefinition = "NVARCHAR(MAX)")
    private String pronunciationDetailsJson;

    @org.hibernate.annotations.Nationalized
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String transcript;

    @Column(name = "score_pronunciation")
    private Integer scorePronunciation;

    @Column(name = "score_grammar")
    private Integer scoreGrammar;

    @Column(name = "score_vocabulary")
    private Integer scoreVocabulary;

    @Column(name = "score_fluency")
    private Integer scoreFluency;

    @Column(name = "score_total")
    private Double scoreTotal;

    @org.hibernate.annotations.Nationalized
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String feedback;

    @Column(precision = 3, scale = 1)
    private BigDecimal score;

    @org.hibernate.annotations.Nationalized
    @Column(name = "admin_feedback", columnDefinition = "NVARCHAR(MAX)")
    private String adminFeedback;

    @org.hibernate.annotations.Nationalized
    @Column(name = "private_note", columnDefinition = "NVARCHAR(MAX)")
    private String privateNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
}
