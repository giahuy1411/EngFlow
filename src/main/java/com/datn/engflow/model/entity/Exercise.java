package com.datn.engflow.model.entity;

import com.datn.engflow.model.enums.ExerciseType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    private static final Logger log = LoggerFactory.getLogger(Exercise.class);
    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Lesson lesson;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 50)
    private ExerciseType exerciseType;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String options; // JSON array of options e.g., ["A", "B", "C", "D"]

    @Column(name = "correct_answer", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String correctAnswer;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String explanation;

    @Builder.Default
    private Integer points = 10;

    @Column(length = 20)
    private String difficulty;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    private void validateCorrectAnswer() {
        if (exerciseType == com.datn.engflow.model.enums.ExerciseType.MULTIPLE_CHOICE) {
            try {
                int index = Integer.parseInt(correctAnswer);
                if (index < 0) {
                    throw new IllegalArgumentException("correctAnswer for MULTIPLE_CHOICE must be a non-negative integer index");
                }
                if (options != null && !options.trim().isEmpty()) {
                    try {
                        java.util.List<?> list = objectMapper.readValue(options, java.util.List.class);
                        if (index >= list.size()) {
                            throw new IllegalArgumentException("correctAnswer index " + index + " out of bounds for options of size " + list.size());
                        }
                    } catch (Exception e) {
                        if (e instanceof IllegalArgumentException) {
                            throw (IllegalArgumentException) e;
                        }
                        log.warn("Failed to parse options JSON for exercise id={}: {}", this.id, e.getMessage());
                    }
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("correctAnswer for MULTIPLE_CHOICE must be a valid integer index string (e.g. '0', '1')");
            }
        }
    }
}
