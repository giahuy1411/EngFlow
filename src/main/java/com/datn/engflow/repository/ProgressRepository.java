package com.datn.engflow.repository;

import com.datn.engflow.model.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Truy cập bảng {@code progress} — tiến độ học của một user trên một lesson.
 *
 * <p>Đọc ở màn danh sách bài (gộp theo lô) và dashboard; ghi khi học viên hoàn thành
 * bài. Các phương thức tra theo cặp (userId, lessonId) trả về MỘT bản ghi nên phụ thuộc
 * vào việc DB không có hai dòng trùng cặp khoá đó.
 */
@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {
    /** Tiến độ của user trên một lesson — dùng khi mở chi tiết/đánh dấu hoàn thành. */
    Optional<Progress> findByUserIdAndLessonId(Long userId, Long lessonId);
    /**
     * Toàn bộ tiến độ của user. Trả về hết trong một truy vấn để tầng gọi tự gộp, tránh
     * hỏi DB theo từng bài; chỉ dùng cho tập bài của một người nên số dòng bị chặn tự nhiên.
     */
    List<Progress> findByUserId(Long userId);
    /**
     * Tiến độ của user cho một tập lesson — batch lookup cho màn danh sách bài, thay cho
     * vòng lặp gọi {@link #findByUserIdAndLessonId} từng bài (N+1).
     */
    List<Progress> findByUserIdAndLessonIdIn(Long userId, Collection<Long> lessonIds);
    /** Đếm số bài user đã hoàn thành — cho dashboard, không kéo entity. */
    Long countByUserIdAndIsCompletedTrue(Long userId);
    /**
     * Xoá tiến độ của một lesson, gọi trước khi xoá lesson để tránh FK 547
     * (FK ở DB là NO_ACTION, {@code ddl-auto=update} không tự thêm cascade).
     */
    void deleteByLessonId(Long lessonId);
}
