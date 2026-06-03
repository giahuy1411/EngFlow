package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vocabulary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vocab_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Lesson lesson;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 100)
    private String word;

    @org.hibernate.annotations.Nationalized
    @Column(length = 100)
    private String pronunciation;

    @org.hibernate.annotations.Nationalized
    @Column(length = 500)
    private String meaning;

    @Column(name = "example_sentence", columnDefinition = "NVARCHAR(MAX)")
    private String exampleSentence;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "word_type", length = 50)
    private String wordType;

    @Column(name = "definition_en", columnDefinition = "NVARCHAR(MAX)")
    private String definitionEn;

    @Column(name = "cefr_level", length = 10)
    private String cefrLevel;

    @Column(name = "source", length = 50)
    private String source;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
