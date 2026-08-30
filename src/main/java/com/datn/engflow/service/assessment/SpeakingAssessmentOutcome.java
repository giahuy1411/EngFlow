package com.datn.engflow.service.assessment;

import com.datn.engflow.service.assessment.TranscriptAlignmentMetrics.AlignmentResult;

/**
 * Outcome of the automatic (non-pronunciation) assessment of one submission.
 *
 * @param transcript   recognized speech text, or the learner-supplied text
 * @param transcriptSource where the transcript came from: {@code WHISPER}, {@code USER}, or {@code NONE}
 * @param alignment    reference-vs-transcript alignment metrics
 * @param rubric       LLM language rubric, or {@code null} when the LLM was unavailable
 * @param provider     identifier of the pipeline that produced this result
 * @param error        non-null when the assessment degraded; the submission still keeps manual grading
 */
public record SpeakingAssessmentOutcome(
        String transcript,
        String transcriptSource,
        AlignmentResult alignment,
        SpeakingRubricResult rubric,
        String provider,
        String error) {

    /**
     * Builds a failed outcome that preserves the manual-grading path.
     *
     * @param provider pipeline identifier
     * @param error    human-readable failure reason
     * @return outcome carrying no scores
     */
    public static SpeakingAssessmentOutcome failed(String provider, String error) {
        return new SpeakingAssessmentOutcome(null, "NONE", null, null, provider, error);
    }
}
