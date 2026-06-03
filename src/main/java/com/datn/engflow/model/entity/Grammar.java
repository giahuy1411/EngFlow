package com.datn.engflow.model.entity;

import com.datn.engflow.model.enums.LessonLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "grammar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grammar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grammar_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Lesson lesson;

    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String explanation;

    @Column(length = 500)
    private String formula;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String examples;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LessonLevel level;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
