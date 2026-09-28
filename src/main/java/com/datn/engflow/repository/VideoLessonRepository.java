package com.datn.engflow.repository;

import com.datn.engflow.model.entity.VideoLesson;
import com.datn.engflow.model.enums.LessonLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Truy cập bảng {@code video_lessons} — bài học video, transcript lưu dạng JSON.
 *
 * <p>Không có {@code @Repository} (Spring Data tự phát hiện). Cả hai truy vấn dưới đây
 * đều trả entity đầy đủ, tức kéo theo cột {@code transcript_json} là
 * {@code NVARCHAR(MAX)}; chấp nhận được vì bài video ít dòng và màn danh sách cần
 * transcript để tính số dòng. Đừng dùng hai hàm này cho bảng lớn.
 */
public interface VideoLessonRepository extends JpaRepository<VideoLesson, Long> {

    /** Catalogue công khai: chỉ bài đã publish, có phân trang. */
    Page<VideoLesson> findByIsPublishedTrue(Pageable pageable);

    /** Catalogue công khai lọc thêm theo trình độ; {@code level} null thì service rơi về bản không lọc. */
    Page<VideoLesson> findByIsPublishedTrueAndLevel(LessonLevel level, Pageable pageable);
}
