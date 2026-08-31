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
        // Average on the 0-10 scale, one decimal: (7+6+8)/3 = 7.0.
        assertThat(result.total()).isEqualTo(7.0);
        assertThat(result.feedback()).contains("thì quá khứ");
    }

    @Test
    void parsesJsonWrappedInMarkdownFence() {
        SpeakingRubricResult result = SpeakingRubricClient.parseRubric(
                "```json\n{\"grammar\":5,\"vocabulary\":5,\"fluency\":5,\"feedback\":\"ok\"}\n```");

        assertThat(result.total()).isEqualTo(5.0);
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

    @Test
    void correctsKnownVietnameseMisspellingsInFeedback() {
        // Regression guard: qwen2.5:3b produced "từ vựt" in production feedback (submission 40013).
        SpeakingRubricResult result = SpeakingRubricClient.parseRubric(
                "{\"grammar\":1,\"vocabulary\":1,\"fluency\":1,"
                        + "\"feedback\":\"Trình độ ngữ pháp và từ vựt của bài đọc rất kém, cần cải thiện.\"}");

        assertThat(result.feedback()).contains("từ vựng");
        assertThat(result.feedback()).doesNotContain("từ vựt");
    }

    @Test
    void sanitizeFixesMisspellingsWithoutDiacritics() {
        assertThat(SpeakingRubricClient.sanitizeVietnameseSpelling(
                "Bạn có kỹ năng ngu phap tốt, phat am chưa rõ."))
                .isEqualTo("Bạn có kỹ năng ngữ pháp tốt, phát âm chưa rõ.");

        assertThat(SpeakingRubricClient.sanitizeVietnameseSpelling(
                "Nên trau dồi từ vựt và cách troi chay của câu.")
                )
                .isEqualTo("Nên trau dồi từ vựng và cách trôi chảy của câu.");
    }

    @Test
    void sanitizeLeavesCorrectSpellingUntouched() {
        String clean = "Bạn dùng từ vựng phong phú, ngữ pháp chính xác.";
        assertThat(SpeakingRubricClient.sanitizeVietnameseSpelling(clean)).isEqualTo(clean);
        assertThat(SpeakingRubricClient.sanitizeVietnameseSpelling("")).isEmpty();
        assertThat(SpeakingRubricClient.sanitizeVietnameseSpelling(null)).isNull();
    }
}
