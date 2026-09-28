package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tiến độ của một user với một {@link Lesson}: mở bài lần cuối, đã hoàn thành hay chưa.
 *
 * <p>Tầng entity. Cặp {@code (user_id, lesson_id)} là duy nhất, nên mỗi user chỉ có
 * một dòng mỗi bài. {@link LessonService} tạo dòng lúc mở bài và cập nhật
 * {@link #lastAccessed}; {@link ProgressService} và {@link DashboardService} tổng
 * hợp từ đây. Lưu ý {@link #completionPercentage} và {@link #isCompleted} được
 * đọc rất nhiều nhưng hiện chưa có service nào ghi giá trị khác 0/false — điểm
 * hoàn thành thật sự đến từ {@link ExerciseAttempt}.
 */
@Entity
@Table(name = "user_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "lesson_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Builder.Default
    @Column(name = "completion_percentage", precision = 5, scale = 2)
    private BigDecimal completionPercentage = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
