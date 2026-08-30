package com.datn.engflow.model.dto.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Video learning DTOs (records).
 */
public final class VideoDtos {

    private VideoDtos() {
    }

    /** One transcript line as stored in video_lessons.transcript_json. */
    public record TranscriptLine(
            double start,
            double end,
            @NotBlank String textEn,
            String textVi) {
    }

    /** Admin create/update payload: youtube url + transcript (JSON lines or SRT/VTT raw). */
    public record VideoLessonRequest(
            @NotBlank String title,
            String description,
            @NotBlank String youtubeUrl,
            @NotBlank String level,
            String category,
            @NotNull List<TranscriptLine> transcript,
            Boolean isPublished) {
    }

    /** Public list item. */
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

    /** Public detail: summary + transcript + completed lines for current user. */
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

    /** Attempt (shadowing recording) as returned to users/admins. */
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

    /** Admin grading payload. */
    public record GradeVideoAttemptRequest(
            @NotNull Double score,
            String feedback) {
    }
}
