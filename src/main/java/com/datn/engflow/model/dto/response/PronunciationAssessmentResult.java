package com.datn.engflow.model.dto.response;

/**
 * Immutable pronunciation metrics on Azure Speech's 0-100 scale.
 *
 * @param accuracy phoneme accuracy
 * @param fluency natural pacing and pauses
 * @param completeness proportion of reference text spoken
 * @param prosody stress, intonation, speed, and rhythm
 * @param overall weighted pronunciation score
 * @param recognizedText transcript recognized from the submitted audio
 * @param detailsJson provider details retained for diagnostics
 */
public record PronunciationAssessmentResult(
        double accuracy,
        double fluency,
        double completeness,
        double prosody,
        double overall,
        String recognizedText,
        String detailsJson) {
}
