package com.datn.engflow.repository;

import com.datn.engflow.model.entity.ExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Truy cập bảng {@code exercise_attempts} — lịch sử người dùng làm bài trong một lesson.
 *
 * <p>{@code ExerciseService} ghi bản ghi mới khi người dùng nộp bài và đọc lại để
 * tính điểm; {@code LessonService} dùng {@link #deleteAllByLessonId} khi xoá
 * lesson để dọn bản ghi con theo.
 */
@Repository
public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, Long> {
    /**
     * Lịch sử làm bài của một người dùng trong một lesson, mới nhất trước.
     *
     * @param userId   id người dùng
     * @param lessonId id lesson
     * @return danh sách lần nộp bài theo thời gian hoàn thành giảm dần
     */
    List<ExerciseAttempt> findByUserIdAndLessonIdOrderByCompletedAtDesc(Long userId, Long lessonId);

    /**
     * Lấy một lần nộp bài nhưng ràng theo chủ sở hữu, để controller không trả
     * nhầm bài của người khác khi đoán id.
     *
     * @param id     id bản ghi nộp bài
     * @param userId id người dùng đang đăng nhập
     * @return bản ghi nộp bài nếu thuộc đúng người dùng đó
     */
    Optional<ExerciseAttempt> findByIdAndUserId(Long id, Long userId);

    /**
     * Xoá toàn bộ lần nộp bài của một lesson, dùng khi xoá lesson.
     *
     * @param lessonId id lesson cần dọn dữ liệu con
     */
    @Modifying
    @Transactional
    void deleteAllByLessonId(Long lessonId);
}
