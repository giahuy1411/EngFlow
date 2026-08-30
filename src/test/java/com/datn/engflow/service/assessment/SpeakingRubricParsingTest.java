package com.datn.engflow.service.assessment;

import com.datn.engflow.service.assessment.SpeakingAssessmentService.SpeakingRubricClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeakingRubricParsingTest {

    @Test
    void parsesPlainJsonRubric() {
        SpeakingRubricResult result = SpeakingRubricClient.parseRubric(
                "{\"grammar\":7,\"vocabulary\":6,\"fluency\":8,\"feedback\":\"Bạn nói tốt, chú ý thì quá khứ.\"}");

        assertThat(result.grammar()).isEqualTo(7);
        assertThat(result.vocabulary()).isEqualTo(6);
        assertThat(result.fluency()).isEqualTo(8);
        assertThat(result.total()).isEqualTo(21);
        assertThat(result.feedback()).contains("thì quá khứ");
    }

    @Test
    void parsesJsonWrappedInMarkdownFence() {
        SpeakingRubricResult result = SpeakingRubricClient.parseRubric(
                "```json\n{\"grammar\":5,\"vocabulary\":5,\"fluency\":5,\"feedback\":\"ok\"}\n```");

        assertThat(result.total()).isEqualTo(15);
    }

    @Test
    void clampsOutOfRangeScores() {
        SpeakingRubricResult result = SpeakingRubricClient.parseRubric(
                "{\"grammar\":17,\"vocabulary\":-3,\"fluency\":9,\"feedback\":\"x\"}");

        assertThat(result.grammar()).isEqualTo(10);
        assertThat(result.vocabulary()).isZero();
    }

    @Test
    void missingFeedbackGetsPlaceholder() {
        SpeakingRubricResult result = SpeakingRubricClient.parseRubric(
                "{\"grammar\":4,\"vocabulary\":4,\"fluency\":4}");

        assertThat(result.feedback()).isNotBlank();
    }

    @Test
    void rejectsNonJsonOutput() {
        assertThatThrownBy(() -> SpeakingRubricClient.parseRubric("I think it was fine"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
