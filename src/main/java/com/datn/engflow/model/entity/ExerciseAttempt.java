package com.datn.engflow.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lần làm bài của một user cho một bài học, lưu điểm và chi tiết từng câu.
 *
 * <p>Tầng entity. Chỉ {@link ExerciseService} tạo dòng này (mỗi lần nộp bài một
 * dòng), rồi {@link LessonService} đọc lịch sử theo
 * {@code findByUserIdAndLessonIdOrderByCompletedAtDesc}. Trường {@link #lessonId}
 * cố ý là {@code Long} thay vì quan hệ {@code @ManyToOne}: lịch sử là dữ liệu bất
 * biến, không cần join bảng lesson và không muốn nó bị xóa theo lesson.
 */
@Entity
@Table(name = "exercise_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String details;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
