package com.datn.engflow.repository;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.dto.projection.ExerciseLessonProjection;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Truy cập bảng {@code exercises} — các câu hỏi thuộc về một lesson.
 *
 * <p>Đọc bởi {@code ExerciseService} (dựng bài, chấm điểm), {@code LessonService},
 * {@code AdminService} và {@code AiExerciseService} (sinh câu hỏi bằng AI);
 * {@code ExerciseFixRunner} và {@code JsonDataSeeder} dùng khi nạp dữ liệu.
 * Bảng này lớn hơn 43k dòng nên các truy vấn danh sách đều tránh kéo theo
 * cột LOB của lesson.
 */
@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    /**
     * Câu hỏi của một lesson theo thứ tự hiển thị, tải kèm {@code lesson} trong
     * cùng truy vấn để tránh N+1 khi dựng bài.
     *
     * @param lessonId id lesson
     * @return danh sách câu hỏi tăng dần {@code orderIndex}
     */
    @Query("SELECT e FROM Exercise e JOIN FETCH e.lesson WHERE e.lesson.id = :lessonId ORDER BY e.orderIndex ASC")
    List<Exercise> findByLessonIdOrderByOrderIndexAsc(@Param("lessonId") Long lessonId);

    /**
     * audit-v9 F108: flat projection for the public/admin exercise LIST. The
     * JOIN FETCH variant above drags lesson.content + lesson.content_original
     * (NVARCHAR(MAX)) into every row of the response; see
     * {@link ExerciseLessonProjection} for the measurements.
     *
     * @param lessonId id lesson
     * @return danh sách projection chỉ gồm cột câu hỏi và thông tin lesson liên quan
     */
    @Query("""
            SELECT e.id AS id, l.id AS lessonId, l.title AS lessonTitle, e.question AS question,
                   e.options AS options, e.correctAnswer AS correctAnswer,
                   e.exerciseType AS exerciseType, e.difficulty AS difficulty,
                   e.explanation AS explanation, e.imageUrl AS imageUrl,
                   e.audioUrl AS audioUrl, e.orderIndex AS orderIndex
            FROM Exercise e
            JOIN e.lesson l
            WHERE l.id = :lessonId
            ORDER BY e.orderIndex ASC
            """)
    List<ExerciseLessonProjection> findLessonExercisesProjection(@Param("lessonId") Long lessonId);

    /**
     * Câu hỏi của lesson lọc theo loại, ví dụ riêng phần nghe hoặc phần nói.
     *
     * @param lessonId    id lesson
     * @param exerciseType tên loại câu hỏi cần lọc
     * @return danh sách câu hỏi tăng dần {@code orderIndex}
     */
    List<Exercise> findByLessonIdAndExerciseTypeOrderByOrderIndexAsc(Long lessonId, String exerciseType);

    /**
     * Câu hỏi của lesson lọc theo độ khó.
     *
     * @param lessonId   id lesson
     * @param difficulty tên độ khó cần lọc
     * @return danh sách câu hỏi tăng dần {@code orderIndex}
     */
    List<Exercise> findByLessonIdAndDifficultyOrderByOrderIndexAsc(Long lessonId, String difficulty);

    /**
     * Trang câu hỏi cho màn hình quản trị: lọc theo lesson, loại, độ khó và từ
     * khóa trong một truy vấn. Điều kiện nullable được xử lý trong JPQL nên
     * controller truyền thẳng giá trị query param xuống được.
     *
     * @param lessonId   id lesson, null hoặc rỗng nghĩa là không lọc
     * @param type       loại câu hỏi, null nghĩa là không lọc
     * @param difficulty độ khó, null nghĩa là không lọc
     * @param keyword    từ khóa tìm trong question và explanation
     * @param pageable   cấu hình phân trang
     * @return trang câu hỏi khớp bộ lọc
     */
    @Query("""
            SELECT e FROM Exercise e
            JOIN e.lesson l
            WHERE (:lessonId IS NULL OR l.id = :lessonId)
              AND (:type IS NULL OR :type = '' OR e.exerciseType = :type)
              AND (:difficulty IS NULL OR :difficulty = '' OR e.difficulty = :difficulty)
              AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(e.question) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(e.explanation, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Exercise> findAdminPage(@Param("lessonId") Long lessonId,
                                 @Param("type") ExerciseType type,
                                 @Param("difficulty") ExerciseDifficulty difficulty,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

    /**
     * Số câu hỏi của một lesson, dùng cho thống kê và kiểm tra ràng buộc khi xoá.
     *
     * @param lessonId id lesson
     * @return số câu hỏi thuộc lesson
     */
    long countByLessonId(Long lessonId);

    /**
     * Xoá toàn bộ câu hỏi của một lesson, dùng khi xoá lesson.
     *
     * @param lessonId id lesson cần dọn câu hỏi
     */
    @Modifying
    @Transactional
    void deleteAllByLessonId(Long lessonId);

    /**
     * Returns exercises that have a non-null question and a missing or empty correctAnswer
     * (the backfill pipeline rewrites these with AI-generated keys). Returns a Page for memory
     * safety on the 43k-row exercises table.
     * JOIN FETCH lesson: the backfill walks candidates outside any transaction and reads
     * e.getLesson() (lazy) — without the fetch the very first access throws
     * LazyInitializationException (bug (a), audit plan P3.1).
     */
    @Query("SELECT e FROM Exercise e JOIN FETCH e.lesson WHERE (e.correctAnswer IS NULL OR TRIM(e.correctAnswer) = '') AND e.question IS NOT NULL AND TRIM(e.question) <> '' ORDER BY e.id ASC")
    List<Exercise> findBackfillCandidates(Pageable pageable);
}
