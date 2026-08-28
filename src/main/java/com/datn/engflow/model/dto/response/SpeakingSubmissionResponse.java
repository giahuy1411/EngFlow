package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.entity.SpeakingSubmission;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * record SpeakingSubmissionResponse.
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
        Integer scoreTotal,
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
    public static SpeakingSubmissionResponse from(SpeakingSubmission submission) {
        return from(submission, submission.getVideoUrl());
    }

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
