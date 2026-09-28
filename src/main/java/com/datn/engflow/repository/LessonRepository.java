package com.datn.engflow.repository;

import com.datn.engflow.model.dto.projection.LessonListProjection;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.LessonLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Truy cập bảng {@code lessons} — đơn vị nội dung gốc của nền tảng.
 *
 * <p>Hai cột {@code content} và {@code content_original} là {@code NVARCHAR(MAX)}
 * (116 MB LOB page). Mọi truy vấn danh sách ở đây vì thế KHÔNG được {@code JOIN FETCH}
 * entity đầy đủ: đo trên 20 dòng, kéo nguyên entity tốn 95k logical reads / 367 ms, còn
 * projection chỉ lấy cột cần dùng thì gần như không đọc LOB. Pattern đúng là JOIN phẳng
 * rồi batch projection ({@link com.datn.engflow.model.dto.projection.LessonTitle},
 * {@link com.datn.engflow.model.dto.projection.LessonListProjection}).
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    /**
     * Toàn bộ bài đã publish theo {@code orderIndex} — đường học tuần tự của học viên.
     * Kéo entity đầy đủ (kèm LOB) nên chỉ dùng cho màn nhỏ; {@code LessonService}
     * gọi đúng một lần rồi map sang response.
     */
    List<Lesson> findByIsPublishedTrueOrderByOrderIndexAsc();
    /** Bài đã publish trong một trình độ. Hiện chưa có call site (0 caller) — giữ lại làm API lọc. */
    List<Lesson> findByLevelAndIsPublishedTrue(LessonLevel level);
    /** Đếm bài đã publish — dùng cho dashboard (không đọc LOB). */
    long countByIsPublishedTrue();
    /**
     * Tra bài theo tiêu đề chính xác (dùng khi seed: bỏ qua nếu đã tồn tại).
     * Tiêu đề không unique nên {@code Optional} có thể ném kết quả bất ngờ nếu dữ liệu trùng.
     */
    Optional<Lesson> findByTitle(String title);
    /** Tìm bài theo tiêu đề chứa từ khóa (không phân biệt hoa thường). Chưa có call site — giữ làm API dự phòng. */
    List<Lesson> findByTitleContainingIgnoreCase(String keyword);

    /**
     * Tìm kiếm ba cột title/description/category, dạng không phân trang — giữ cho
     * đường cũ; overload có {@link Pageable} mới là đường dùng thật.
     * Lưu ý: {@code %kw%} ở đầu nên index trên title không dùng được, đây là scan có chủ đích.
     */
    @Query("SELECT l FROM Lesson l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(l.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Lesson> searchByKeyword(@Param("keyword") String keyword);

    /** Bản phân trang của {@link #searchByKeyword(String)} — dùng cho danh sách bài công khai. */
    @Query("SELECT l FROM Lesson l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(l.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Lesson> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Chi tiết bài kèm danh sách từ vựng, một truy vấn thay vì N+1.
     *
     * <p>{@code DISTINCT} là bắt buộc: {@code JOIN FETCH} một collection nhân dòng theo
     * số từ vựng, thiếu nó thì {@code Lesson} bị trả trùng. Truy vấn này CỐ Ý kéo cả cột
     * LOB {@code content} vì màn chi tiết cần nội dung bài — chỉ dùng cho MỘT bài theo id,
     * không bao giờ dùng cho danh sách.
     *
     * <p>Không tự lọc {@code isPublished}: việc chặn bản nháp (audit-v8 F88) do
     * {@code LessonService} quyết định để admin vẫn xem trước được.
     */
    @Query("SELECT DISTINCT l FROM Lesson l " +
           "LEFT JOIN FETCH l.vocabularies " +
           "WHERE l.id = :id")
    Optional<Lesson> findByIdWithDetails(@Param("id") Long id);

    /**
     * Chỉ lấy id + title, phục vụ gắn nhãn bài học ở trang bài tập của admin.
     * Chọn nguyên entity sẽ kéo theo {@code lesson.content} và {@code lesson.content_original}
     * (NVARCHAR(MAX), 116 MB LOB page) vào mọi dòng: đo được 320 LOB logical reads và 32ms
     * cho một trang 20 dòng, so với 0 read và 1ms khi chỉ chọn title.
     */
    @Query("SELECT l.id AS lessonId, l.title AS title FROM Lesson l WHERE l.id IN :ids")
    List<com.datn.engflow.model.dto.projection.LessonTitle> findTitlesById(@Param("ids") java.util.Collection<Long> ids);

    /**
     * Danh sách bài đã publish, có lọc keyword/level và phân trang.
     * Hiện chưa có call site (0 caller) — {@code LessonService} dùng bản projection
     * {@link #findPublishedPageProjection} để khỏi kéo cột LOB. Giữ làm API dự phòng.
     */
    @Query("""
            SELECT l FROM Lesson l
            WHERE l.isPublished = true
              AND (:level IS NULL OR l.level = :level)
              AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Lesson> findPublishedPage(@Param("keyword") String keyword, @Param("level") LessonLevel level, Pageable pageable);

    /**
     * Truy vấn danh sách nhẹ — chỉ chọn các cột màn danh sách bài học thật sự render,
     * bỏ qua hai cột {@code NVARCHAR(MAX)} của bảng này (tiết kiệm rất nhiều read).
     * Đây là đường chính của danh sách bài công khai.
     */
    @Query("""
            SELECT l.id AS id, l.title AS title, l.description AS description,
                   l.level AS level, l.category AS category,
                   l.durationMinutes AS durationMinutes, l.thumbnailUrl AS thumbnailUrl,
                   l.audioUrl AS audioUrl, l.skillType AS skillType, l.orderIndex AS orderIndex
            FROM Lesson l
            WHERE l.isPublished = true
              AND (:level IS NULL OR l.level = :level)
              AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<LessonListProjection> findPublishedPageProjection(@Param("keyword") String keyword,
                                                           @Param("level") LessonLevel level,
                                                           Pageable pageable);

    /**
     * Danh sách admin, gồm cả bản nháp; khác {@link #findPublishedPageProjection} ở chỗ
     * KHÔNG lọc {@code isPublished}.
     *
     * <p>Cẩn thận: trả entity {@code Lesson} đầy đủ, tức mỗi dòng kéo theo hai cột LOB —
     * đúng cái pattern đã đo 95k logical reads / 367 ms cho 20 dòng nếu bị đem đi
     * {@code JOIN FETCH} hoặc gọi với số dòng lớn. Admin hiện gọi qua
     * {@code AdminService.getAllLessonsAdmin} rồi map sang DTO tóm tắt; đừng thêm
     * {@code JOIN FETCH} vào đây.
     */
    @Query("""
            SELECT l FROM Lesson l
            WHERE (:level IS NULL OR l.level = :level)
              AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Lesson> findAdminPage(@Param("keyword") String keyword, @Param("level") LessonLevel level, Pageable pageable);
}
