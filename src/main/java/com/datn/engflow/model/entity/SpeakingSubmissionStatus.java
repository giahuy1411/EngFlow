package com.datn.engflow.model.entity;

/**
 * Trạng thái vòng đời của một bài nộp nói và quá trình chấm của nó.
 *
 * <p>Luồng do backend điều khiển: {@code SUBMITTED} lúc người học nộp →
 * {@code PROCESSING} khi chạy pipeline Whisper/Ollama → {@code COMPLETED} khi có
 * rubric, {@code FAILED} khi transcribe hoặc LLM lỗi, {@code GRADED} khi admin
 * chấm tay. {@code UPLOADED} và {@code UNDER_REVIEW} chỉ tồn tại trong ràng buộc
 * CHECK của DB và ở bộ lọc của trang quản trị, không có writer trong Java.
 *
 * <p>Lưu ý khi thêm hằng mới: CHECK constraint ở DB chỉ được nới bởi
 * {@code db/migration/V4__expand_video_submission_status.sql}, được áp dụng lúc
 * khởi động bởi {@code SpeakingSubmissionStatusMigration}.
 */
public enum SpeakingSubmissionStatus {
    UPLOADED,
    PROCESSING,
    COMPLETED,
    FAILED,
    SUBMITTED,
    UNDER_REVIEW,
    GRADED
}
