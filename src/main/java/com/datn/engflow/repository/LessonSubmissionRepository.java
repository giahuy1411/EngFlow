package com.datn.engflow.repository;

import com.datn.engflow.model.entity.LessonSubmission;
import com.datn.engflow.model.enums.SkillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Truy cập bảng {@code lesson_submissions} — bài nộp theo kỹ năng, chờ admin chấm tay.
 *
 * <p>Ghi bởi {@code LessonSubmissionService} (mỗi lần nộp lại thì xoá điểm cũ, đưa
 * {@code status} về PENDING); đọc bởi {@code AdminService} cho hàng đợi chấm bài.
 * Việc xoá dọn dẹp nằm trong {@code LessonService.deleteLesson} vì FK ở DB không
 * khai {@code ON DELETE CASCADE}.
 */
@Repository
public interface LessonSubmissionRepository extends JpaRepository<LessonSubmission, Long> {
    /**
     * Bài nộp của một user cho một lesson theo từng kỹ năng — dùng để tái sử dụng
     * bản ghi khi học viên nộp lại, tránh sinh thêm dòng trùng.
     *
     * <p>Trả {@code Optional} nhưng không ràng buộc unique ở DB, nên nếu lịch sử có
     * nhiều dòng khớp thì Spring Data sẽ ném kết quả không xác định.
     */
    Optional<LessonSubmission> findByUserIdAndLessonIdAndSkillType(Long userId, Long lessonId, SkillType skillType);

    /**
     * Xoá toàn bộ bài nộp của một lesson, gọi trước khi xoá lesson để tránh FK 547
     * (con trỏ tới lessons là NO_ACTION, {@code ddl-auto=update} không tự thêm cascade).
     */
    void deleteByLessonId(Long lessonId);
}
