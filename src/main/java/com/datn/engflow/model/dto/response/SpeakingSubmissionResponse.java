package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.entity.SpeakingSubmission;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một bài nộp Speaking Coach kèm kết quả chấm.
 *
 * <p>Tầng response của toàn bộ nhóm endpoint {@code /api/v1/speaking-submissions}
 * và {@code /api/v1/admin/speaking-submissions}. Hai trường chấm tách bạch: điểm AI
 * tự động (pronunciation/grammar/vocabulary/fluency + 4 chỉ số phát âm) và điểm
 * admin chấm tay ghi đè khi có {@code score}/{@code adminFeedback}. Người chấm
 * được gọn còn {@link UserSummaryResponse}.
 */
public record SpeakingSubmissionResponse(
        Long id,
        UserSummaryResponse user,
        Long promptId,
        String promptTitle,
        String videoUrl,
        String mediaType,
        String status,
        String transcript,
        Integer scorePronunciation,
        Integer scoreGrammar,
        Integer scoreVocabulary,
        Integer scoreFluency,
        Double scoreTotal,
        Double pronunciationAccuracy,
        Double pronunciationFluency,
        Double pronunciationCompleteness,
        Double pronunciationProsody,
        String feedback,
        String assessmentError,
        BigDecimal score,
        String adminFeedback,
        UserSummaryResponse gradedBy,
        LocalDateTime gradedAt,
        LocalDateTime submittedAt
) {
    /**
     * Ánh xạ bài nộp, dùng luôn URL media đã lưu trong entity.
     *
     * @param submission bài nộp cần ánh xạ
     * @return response với mediaUrl lấy từ {@code submission.getVideoUrl()}
     */
    public static SpeakingSubmissionResponse from(SpeakingSubmission submission) {
        return from(submission, submission.getVideoUrl());
    }

    /**
     * Ánh xạ bài nộp với URL media do bên gọi cung cấp.
     *
     * <p>Đường admin và đường người dùng đều đi qua đây nên có thể thay URL bằng
     * signed URL ngắn hạn từ
     * {@code SpeakingSubmissionService.createMediaReadUrl} thay vì URL dài hạn
     * đã lưu.
     *
     * @param submission bài nộp cần ánh xạ
     * @param mediaUrl url media sẽ đưa vào trường videoUrl của response
     * @return response đã ánh xạ
     */
    public static SpeakingSubmissionResponse from(SpeakingSubmission submission, String mediaUrl) {
        return new SpeakingSubmissionResponse(
                submission.getId(),
                UserSummaryResponse.from(submission.getUser()),
                submission.getPrompt().getId(),
                submission.getPrompt().getTitle(),
                mediaUrl,
                submission.getMediaType(),
                submission.getStatus() == null ? null : submission.getStatus().name(),
                submission.getTranscript(),
                submission.getScorePronunciation(),
                submission.getScoreGrammar(),
                submission.getScoreVocabulary(),
                submission.getScoreFluency(),
                submission.getScoreTotal(),
                submission.getPronunciationAccuracy(),
                submission.getPronunciationFluency(),
                submission.getPronunciationCompleteness(),
                submission.getPronunciationProsody(),
                submission.getFeedback(),
                submission.getAssessmentError(),
                submission.getScore(),
                submission.getAdminFeedback(),
                submission.getGradedBy() == null ? null : UserSummaryResponse.from(submission.getGradedBy()),
                submission.getGradedAt(),
                submission.getSubmittedAt()
        );
    }
}
