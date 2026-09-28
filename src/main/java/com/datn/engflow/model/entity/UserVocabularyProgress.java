package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Lịch ôn cá nhân của một user cho một {@link Vocabulary}, theo thuật toán SM-2
 * rút gọn: khoảng cách lần sau, hệ số độ dễ và mức thuộc lòng.
 *
 * <p>Tầng entity. Cặp {@code (user_id, vocabulary_id)} là duy nhất. {@link SrsService}
 * là nơi duy nhất ghi các trường SM-2 sau khi người học ôn; {@link FlashcardService}
 * chỉ đọc để dựng phiên ôn. Bản ghi được tạo ở {@link SrsService} khi lần đầu gặp
 * từ đó, nên có thể chưa tồn tại với mọi từ.
 */
@Entity
@Table(name = "user_vocabulary_progress", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "vocabulary_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVocabularyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    private Vocabulary vocabulary;

    @Builder.Default
    @Column(name = "mastery_level")
    private Integer masteryLevel = 0; // 0: New, 1: Learning, 2: Almost known, 3: Mastered

    @Column(name = "next_review_date")
    private LocalDateTime nextReviewDate;

    @Builder.Default
    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Builder.Default
    @Column(name = "ease_factor")
    private Double easeFactor = 2.5;

    @Builder.Default
    @Column(name = "srs_interval")
    private Integer interval = 1;

    @Builder.Default
    @Column(name = "repetitions")
    private Integer repetitions = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
