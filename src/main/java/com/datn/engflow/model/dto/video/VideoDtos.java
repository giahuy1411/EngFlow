package com.datn.engflow.model.dto.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Nhóm DTO (record) cho tính năng học qua video (video learning).
 *
 * <p>Gom trong một class chỉ để chứa các record — không có instance (constructor
 * private). Ba nhóm: payload admin ({@link VideoLessonRequest}), payload chấm
 * ({@link GradeVideoAttemptRequest}), và response ({@link VideoLessonSummary},
 * {@link VideoLessonDetail}, {@link VideoAttemptResponse}).
 */
public final class VideoDtos {

    private VideoDtos() {
    }

    /** Một dòng phụ đề như lưu trong {@code video_lessons.transcript_json}. */
    public record TranscriptLine(
            double start,
            double end,
            @NotBlank String textEn,
            String textVi) {
    }

    /** Payload admin tạo/sửa bài: youtube url + phụ đề (JSON lines hoặc SRT/VTT thô). */
    public record VideoLessonRequest(
            @NotBlank String title,
            String description,
            @NotBlank String youtubeUrl,
            @NotBlank String level,
            String category,
            // audit-v8 F88: null = "giu nguyen phu de hien co" khi UPDATE (audit-v6 F21) nen KHONG
            // duoc @NotNull o day — annotation do bien guard null trong service thanh dead code va
            // moi lan admin sua bai (bo trong o phu de) deu 400. CREATE van bi chan boi
            // VideoLessonService.validateTranscript: 400 "Transcript can it nhat 2 dong".
            List<TranscriptLine> transcript,
            Boolean isPublished) {
    }

    /** Mục trong danh sách công khai. */
    public record VideoLessonSummary(
            Long id,
            String title,
            String description,
            String youtubeVideoId,
            String level,
            String category,
            Integer durationSeconds,
            int lineCount,
            Boolean isPublished) {
    }

    /** Chi tiết công khai: summary + phụ đề + các dòng người dùng đã hoàn thành. */
    public record VideoLessonDetail(
            Long id,
            String title,
            String description,
            String youtubeVideoId,
            String level,
            String category,
            Integer durationSeconds,
            List<TranscriptLine> transcript,
            List<Integer> completedLines) {
    }

    /** Lượt nộp (bản ghi shadowing) trả cho người dùng/admin. */
    public record VideoAttemptResponse(
            Long id,
            Long videoLessonId,
            Integer lineIndex,
            String status,
            Double score,
            String adminFeedback,
            String mediaUrl,
            String submittedAt) {
    }

    /** Payload admin chấm điểm. */
    public record GradeVideoAttemptRequest(
            @NotNull Double score,
            String feedback) {
    }
}
