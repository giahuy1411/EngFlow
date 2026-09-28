package com.datn.engflow.model.dto.projection;

import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;

/**
 * Projection phẳng cho danh sách bài tập của một bài học (audit-v9 F108).
 *
 * <p>Vì sao tồn tại: trước đây danh sách hydrate entity {@code Exercise} qua
 * {@code JOIN FETCH e.lesson}, nên MỖI dòng kéo theo cả cột {@code content} và
 * {@code content_original} NVARCHAR(MAX) của lesson. Đo ngày 2026-09-17 trên
 * lesson 651 (đã publish, 99 bài tập, content 26 kB + content_original 77 kB):
 * ~5 724 logical reads và ~14 ms server time mỗi lần gọi, median 153 ms; trong
 * khi lesson nhỏ chỉ ~120 reads. Lần thực thi tệ nhất trong plan cache lên tới
 * 197 337 reads / 3 250 ms.
 *
 * <p>Projection này chỉ chọn các cột mà {@code ExerciseResponse} render — cùng
 * kỹ thuật đã dùng ở {@link LessonListProjection} / {@link LessonTitle}.
 */
public interface ExerciseLessonProjection {

    /** @return id bài tập. */
    Long getId();

    /** @return id bài học chứa bài tập. */
    Long getLessonId();

    /** @return tiêu đề bài học (phẳng, không cần join entity). */
    String getLessonTitle();

    /** @return nội dung câu hỏi. */
    String getQuestion();

    /** @return chuỗi JSON các phương án (format tuỳ {@link ExerciseType}). */
    String getOptions();

    /** @return đáp án đúng (format tuỳ {@link ExerciseType}). */
    String getCorrectAnswer();

    /** @return loại bài tập. */
    ExerciseType getExerciseType();

    /** @return độ khó. */
    ExerciseDifficulty getDifficulty();

    /** @return lời giải thích hiển thị sau khi nộp. */
    String getExplanation();

    /** @return URL ảnh minh hoạ (nếu có). */
    String getImageUrl();

    /** @return URL audio (bắt buộc với {@code LISTENING}). */
    String getAudioUrl();

    /** @return thứ tự hiển thị trong bài. */
    Integer getOrderIndex();
}
