package com.datn.engflow.repository;

import com.datn.engflow.model.entity.VideoAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Truy cập bảng {@code video_attempts} — mỗi dòng là một dòng phụ đề (lineIndex) mà
 * người học đã shadow xong trong một bài video.
 *
 * <p>Không có {@code @Repository} vì Spring Data tự phát hiện interface
 * {@code JpaRepository}; service dùng nó để dựng tiến độ "đã nói được mấy dòng".
 */
public interface VideoAttemptRepository extends JpaRepository<VideoAttempt, Long> {

    /** Lịch sử luyện của một user, có phân trang — màn "bài đã luyện" của học viên. */
    Page<VideoAttempt> findByUserId(Long userId, Pageable pageable);

    /** Danh sách bản ghi theo trạng thái, có phân trang — màn duyệt của admin. */
    Page<VideoAttempt> findByStatus(String status, Pageable pageable);

    /**
     * audit-v7 F55: fallback kiểm tra quyền sở hữu cho URL media kiểu cũ. Media proxy
     * đối chiếu (objectKey, userId) trước khi ký/trả file, tránh lộ bản ghi âm của
     * người khác khi object key không chứa userId.
     */
    boolean existsByMediaObjectKeyAndUserId(String mediaObjectKey, Long userId);

    /**
     * Các dòng phụ đề user đã hoàn thành trong một bài video. Chỉ trả {@code lineIndex}
     * (không kéo entity) để player đánh dấu dòng đã luyện mà không tải cả bảng.
     *
     * <p>Không lọc trạng thái — mọi bản ghi của cặp (user, lesson) đều tính là đã luyện;
     * đổi luật này thì phải sửa cả service dựng tiến độ.
     */
    @Query("select a.lineIndex from VideoAttempt a where a.user.id = :userId and a.videoLesson.id = :lessonId")
    List<Integer> findCompletedLineIndexes(@Param("userId") Long userId, @Param("lessonId") Long lessonId);
}
