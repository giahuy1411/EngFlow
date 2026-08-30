package com.datn.engflow.service.assessment;

import com.datn.engflow.service.assessment.TranscriptAlignmentMetrics.AlignmentResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptAlignmentMetricsTest {

    @Test
    void perfectTranscriptScoresZeroErrorFullCoverage() {
        AlignmentResult result = TranscriptAlignmentMetrics.compute(
                "The weather is nice today", "the weather is nice today");

        assertThat(result.wordErrorRatePercent()).isZero();
        assertThat(result.coveragePercent()).isEqualTo(100.0);
        assertThat(result.substitutions()).isZero();
        assertThat(result.insertions()).isZero();
        assertThat(result.deletions()).isZero();
    }

    @Test
    void oneSubstitutionProducesProportionalErrorRate() {
        AlignmentResult result = TranscriptAlignmentMetrics.compute(
                "The weather is nice today", "the weather was nice today");

        assertThat(result.substitutions()).isEqualTo(1);
        assertThat(result.wordErrorRatePercent()).isEqualTo(20.0);
        assertThat(result.coveragePercent()).isEqualTo(80.0);
    }

    @Test
    void missingWordsCountAsDeletions() {
        AlignmentResult result = TranscriptAlignmentMetrics.compute(
                "I enjoy reading books", "i enjoy");

        assertThat(result.deletions()).isEqualTo(2);
        // 2 deletions out of a 4-word reference => WER 50%, coverage 50%.
        assertThat(result.wordErrorRatePercent()).isEqualTo(50.0);
        assertThat(result.coveragePercent()).isEqualTo(50.0);
    }

    @Test
    void extraWordsCountAsInsertions() {
        AlignmentResult result = TranscriptAlignmentMetrics.compute(
                "I like coffee", "i really like coffee a lot");

        assertThat(result.insertions()).isEqualTo(3);
        assertThat(result.deletions()).isZero();
        assertThat(result.substitutions()).isZero();
    }

    @Test
    void punctuationAndCaseAreIgnored() {
        AlignmentResult result = TranscriptAlignmentMetrics.compute(
                "Don't stop, believe!", "dont stop believe");

        assertThat(result.wordErrorRatePercent()).isZero();
    }

    @Test
    void emptyTranscriptAgainstReferenceIsTotalFailure() {
        AlignmentResult result = TranscriptAlignmentMetrics.compute("Hello world", "   ");

        assertThat(result.wordErrorRatePercent()).isEqualTo(100.0);
        assertThat(result.coveragePercent()).isZero();
        assertThat(result.deletions()).isEqualTo(2);
    }

    @Test
    void unscriptedTaskWithoutReferenceYieldsNeutralMetrics() {
        AlignmentResult result = TranscriptAlignmentMetrics.compute(null, "whatever i said");

        assertThat(result.wordErrorRatePercent()).isZero();
        assertThat(result.coveragePercent()).isZero();
        assertThat(result.hypothesisWords()).isEqualTo(3);
    }
}
