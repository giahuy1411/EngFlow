package com.datn.engflow.service;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.enums.ExerciseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AiAnswerBackfillService answer-key parsing, fragment
 * classification and untrusted-AI-output validation (LLM05 posture).
 */
class AiAnswerBackfillServiceTest {

    // ── parseAnswerKey ──

    @Test
    @DisplayName("parseAnswerKey: letter key '1 b 2 a 3 b' inside details ANSWER")
    void parsesLetterKey() {
        String html = """
                <h3>Listening 1</h3>
                <details> <summary>ANSWER</summary> <p><strong>1 </strong>b <strong>2</strong> a <strong>3</strong> b <strong>4 </strong>b <strong>5</strong> a <strong>6</strong> a</p> </details>
                """;
        Map<Integer, String> key = AiAnswerBackfillService.parseAnswerKeyMap(html);
        assertThat(key).containsEntry(1, "b").containsEntry(2, "a")
                       .containsEntry(4, "b").containsEntry(6, "a").hasSize(6);
    }

    @Test
    @DisplayName("parseAnswerKey: word key '1taller 2 larger 3 hotter'")
    void parsesWordKey() {
        String html = """
                <details> <summary>ANSWER</summary> <p> 1taller 2 larger 3 hotter 4 earlier</p> <p> 5more powerful 6 more spectacular</p> </details>
                """;
        Map<Integer, String> key = AiAnswerBackfillService.parseAnswerKeyMap(html);
        assertThat(key).containsEntry(1, "taller").containsEntry(2, "larger")
                       .containsEntry(5, "more powerful").hasSize(6);
    }

    @Test
    @DisplayName("parseAnswerKey: sentence key and phrase-with-comma key")
    void parsesSentenceAndPhraseKeys() {
        String html = """
                <details> <summary>ANSWER</summary> <p> 1 Jack Black is the funniest actor in Hollywood.</p> <p> 2 English is the most difficult subject.</p> </details>
                """;
        Map<Integer, String> key = AiAnswerBackfillService.parseAnswerKeyMap(html);
        assertThat(key).containsEntry(1, "Jack Black is the funniest actor in Hollywood.")
                       .containsEntry(2, "English is the most difficult subject.");

        String html2 = """
                <details> <summary>ANSWER</summary> <p> 1 spend, on 2 sit at 3 work for</p> </details>
                """;
        Map<Integer, String> key2 = AiAnswerBackfillService.parseAnswerKeyMap(html2);
        assertThat(key2).containsEntry(1, "spend, on").containsEntry(2, "sit at").containsEntry(3, "work for");
    }

    @Test
    @DisplayName("parseAnswerKey: empty/blank content returns empty map")
    void emptyContent() {
        assertThat(AiAnswerBackfillService.parseAnswerKeyMap(null)).isEmpty();
        assertThat(AiAnswerBackfillService.parseAnswerKeyMap("")).isEmpty();
        assertThat(AiAnswerBackfillService.parseAnswerKeyMap("<p>no answer here</p>")).isEmpty();
    }

    // ── leadingExerciseNumber ──

    @Test
    @DisplayName("leadingExerciseNumber: embedded '1……' '12……' '2 a …' prefixes")
    void leadingNumbers() {
        assertThat(AiAnswerBackfillService.leadingExerciseNumber("1……………………….. (die)")).isEqualTo(1);
        assertThat(AiAnswerBackfillService.leadingExerciseNumber("12………………… (use)")).isEqualTo(12);
        assertThat(AiAnswerBackfillService.leadingExerciseNumber("2 a His friend never lends things.")).isEqualTo(2);
        assertThat(AiAnswerBackfillService.leadingExerciseNumber("He would rather play tennis.")).isNull();
    }

    // ── isFragmentQuestion (MC classification) ──

    @Test
    @DisplayName("isFragmentQuestion: fragments vs self-contained prompts")
    void fragmentClassification() {
        // fragments → true
        assertThat(AiAnswerBackfillService.isFragmentQuestion("a He said that he wasn’t Tom Cruise.")).isTrue();
        assertThat(AiAnswerBackfillService.isFragmentQuestion("gold and silver")).isTrue();
        assertThat(AiAnswerBackfillService.isFragmentQuestion("……")).isTrue();
        assertThat(AiAnswerBackfillService.isFragmentQuestion("fit")).isTrue();
        assertThat(AiAnswerBackfillService.isFragmentQuestion("few 7 little")).isTrue();
        assertThat(AiAnswerBackfillService.isFragmentQuestion("helps you make funny photos")).isTrue();
        // self-contained → false
        assertThat(AiAnswerBackfillService.isFragmentQuestion("Is Sharon there?")).isFalse();
        assertThat(AiAnswerBackfillService.isFragmentQuestion("…… criminals can use their knowledge of technology to commit crimes.")).isFalse();
        assertThat(AiAnswerBackfillService.isFragmentQuestion("A d__________ involves goods, letters, parcels.")).isFalse();
        assertThat(AiAnswerBackfillService.isFragmentQuestion("Max is pretending ………………… asleep, but he isn’t really. (be)")).isFalse();
    }

    // ── isValidAnswer (untrusted AI output) ──

    private Exercise fb(String question) {
        return Exercise.builder().question(question).exerciseType(ExerciseType.FILL_BLANK).build();
    }

    private Exercise mc(String question) {
        return Exercise.builder().question(question).exerciseType(ExerciseType.MULTIPLE_CHOICE).build();
    }

    @Test
    @DisplayName("isValidAnswer: accepts clean words, rejects junk shapes")
    void validationAcceptsRejects() {
        assertThat(AiAnswerBackfillService.isValidAnswer("got", fb("Tom …… (get) a surprise"))).isTrue();
        assertThat(AiAnswerBackfillService.isValidAnswer("more powerful", fb("…… powerful"))).isTrue();
        assertThat(AiAnswerBackfillService.isValidAnswer("spend, on", fb("I …… too much …… games"))).isTrue();
        // junk
        assertThat(AiAnswerBackfillService.isValidAnswer("……", fb("……"))).isFalse();
        assertThat(AiAnswerBackfillService.isValidAnswer("left|right", mc("match"))).isFalse();
        assertThat(AiAnswerBackfillService.isValidAnswer("answer", fb("gap"))).isFalse();
        assertThat(AiAnswerBackfillService.isValidAnswer("b", mc("Is it correct?"))).isFalse();
        assertThat(AiAnswerBackfillService.isValidAnswer("b He doesn’t want his guest", mc("x"))).isFalse();
        assertThat(AiAnswerBackfillService.isValidAnswer("(get)", fb("Tom …… (get)"))).isFalse();
        assertThat(AiAnswerBackfillService.isValidAnswer("", fb("x"))).isFalse();
        assertThat(AiAnswerBackfillService.isValidAnswer(null, fb("x"))).isFalse();
        assertThat(AiAnswerBackfillService.isValidAnswer("x".repeat(121), fb("gap"))).isFalse();
    }

    @Test
    @DisplayName("isValidAnswer: multi-gap FILL_BLANK requires comma-joined answers")
    void multiGapValidation() {
        assertThat(AiAnswerBackfillService.isValidAnswer("get", fb("He …… a surprise when he …… the phone"))).isFalse();
        assertThat(AiAnswerBackfillService.isValidAnswer("got, answered", fb("He …… a surprise when he …… the phone"))).isTrue();
    }

    // ── sanitizeAiOutput ──

    @Test
    @DisplayName("sanitizeAiOutput: strips labels, quotes, fences, extra lines")
    void sanitize() {
        assertThat(AiAnswerBackfillService.sanitizeAiOutput("got")).isEqualTo("got");
        assertThat(AiAnswerBackfillService.sanitizeAiOutput("Answer: got")).isEqualTo("got");
        assertThat(AiAnswerBackfillService.sanitizeAiOutput("\"got\"")).isEqualTo("got");
        assertThat(AiAnswerBackfillService.sanitizeAiOutput("```json\ngot\n```")).isEqualTo("got");
        assertThat(AiAnswerBackfillService.sanitizeAiOutput("got\nbecause 3rd person")).isEqualTo("got");
        assertThat(AiAnswerBackfillService.sanitizeAiOutput("   ")).isNull();
        assertThat(AiAnswerBackfillService.sanitizeAiOutput(null)).isNull();
    }

    // ── countGaps ──

    @Test
    @DisplayName("countGaps: counts …-gaps and ___-gaps")
    void gapCounting() {
        assertThat(AiAnswerBackfillService.countGaps("I …… meat, but I …… fish.")).isEqualTo(2);
        assertThat(AiAnswerBackfillService.countGaps("A d__________ involves goods.")).isEqualTo(1);
        assertThat(AiAnswerBackfillService.countGaps("No gap here.")).isZero();
        assertThat(AiAnswerBackfillService.countGaps(null)).isZero();
    }

    // ── parseAnswerBlocks (block-aware alignment regression) ──

    @Test
    @DisplayName("parseAnswerBlocks: mỗi block giữ số riêng, không flatten")
    void blockAwareParsing() {
        String html = """
                <details> <summary>ANSWER</summary> <p><strong>1</strong> studied <strong>2</strong> moved</p> </details>
                <details> <summary>ANSWER</summary> <p><strong>1</strong> chose <strong>2</strong> found</p> </details>
                """;
        java.util.List<Map<Integer, String>> blocks = AiAnswerBackfillService.parseAnswerBlocks(html);
        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0)).containsEntry(1, "studied").containsEntry(2, "moved");
        assertThat(blocks.get(1)).containsEntry(1, "chose").containsEntry(2, "found");
    }

    // ── embeddedGapNumbers ──

    @Test
    @DisplayName("embeddedGapNumbers: 'Crist11……' 'he12……' trích đúng số gap")
    void embeddedGapNumberExtraction() {
        assertThat(AiAnswerBackfillService.embeddedGapNumbers(
                "Tom Crist11……………………….. (get) a big surprise when he12……………………….. (answer)"))
                .containsExactly(11, 12);
        assertThat(AiAnswerBackfillService.embeddedGapNumbers("No numbers …… here")).isEmpty();
        assertThat(AiAnswerBackfillService.embeddedGapNumbers(null)).isEmpty();
    }

    // ── answersForNumbers ──

    @Test
    @DisplayName("answersForNumbers: ưu tiên block hiện tại, join phẩy")
    void answersForNumbersPreferredBlock() {
        java.util.List<Map<Integer, String>> blocks = AiAnswerBackfillService.parseAnswerBlocks("""
                <details> <summary>ANSWER</summary> <p><strong>1</strong> was <strong>2</strong> won <strong>11</strong> got <strong>12</strong> answered</p> </details>
                <details> <summary>ANSWER</summary> <p><strong>11</strong> wrong <strong>12</strong> wrong</p> </details>
                """);
        // preferredIdx=0: gap 11/12 resolve trong block 0.
        assertThat(AiAnswerBackfillService.answersForNumbers(blocks, 0, java.util.List.of(11, 12)))
                .isEqualTo("got, answered");
        // preferredIdx=1: block 1 có 11/12 → dùng block 1.
        assertThat(AiAnswerBackfillService.answersForNumbers(blocks, 1, java.util.List.of(11, 12)))
                .isEqualTo("wrong, wrong");
        // Số thiếu → null.
        assertThat(AiAnswerBackfillService.answersForNumbers(blocks, 0, java.util.List.of(11, 99))).isNull();
    }

    // ── isAnswerKeyFragment ──

    @Test
    @DisplayName("isAnswerKeyFragment: 'studied 2 moved 3 looked' là key fragment")
    void answerKeyFragmentDetection() {
        assertThat(AiAnswerBackfillService.isAnswerKeyFragment(
                "studied 2 moved 3 looked 4 stopped 5 talked")).isTrue();
        assertThat(AiAnswerBackfillService.isAnswerKeyFragment("answered 13 was 14 was 15 decided 16 gave")).isTrue();
        // Ngày tháng ("December 2, 1992") không phải key fragment.
        assertThat(AiAnswerBackfillService.isAnswerKeyFragment("He called me on December 2, 1992")).isFalse();
        assertThat(AiAnswerBackfillService.isAnswerKeyFragment("Is Sharon there?")).isFalse();
        assertThat(AiAnswerBackfillService.isAnswerKeyFragment(null)).isFalse();
    }

    // ── takeSlots ──

    @Test
    @DisplayName("takeSlots: multi-gap FB consume N slots liên tiếp trong block")
    void takeSlotsConsumesConsecutive() {
        java.util.List<Map<Integer, String>> blocks = AiAnswerBackfillService.parseAnswerBlocks("""
                <details> <summary>ANSWER</summary> <p><strong>1</strong> was <strong>2</strong> won <strong>3</strong> gave <strong>4</strong> started</p> </details>
                """);
        java.util.List<String> slots = AiAnswerBackfillService.takeSlots(blocks, 0, 0, 2, null);
        assertThat(slots).containsExactly("was", "won");
        java.util.List<String> next = AiAnswerBackfillService.takeSlots(blocks, 0, 2, 2, null);
        assertThat(next).containsExactly("gave", "started");
        // Hết block → null.
        assertThat(AiAnswerBackfillService.takeSlots(blocks, 0, 4, 1, null)).isNull();
        assertThat(AiAnswerBackfillService.takeSlots(blocks, 5, 0, 1, null)).isNull();
    }

    // ── lookupNumbered ──

    @Test
    @DisplayName("lookupNumbered: tìm số xuyên block theo document order")
    void lookupNumberedAcrossBlocks() {
        java.util.List<Map<Integer, String>> blocks = AiAnswerBackfillService.parseAnswerBlocks("""
                <details> <summary>ANSWER</summary> <p><strong>1</strong> a <strong>2</strong> b</p> </details>
                <details> <summary>ANSWER</summary> <p><strong>1</strong> x <strong>3</strong> z</p> </details>
                """);
        assertThat(AiAnswerBackfillService.lookupNumbered(blocks, 1)).isEqualTo("a");
        assertThat(AiAnswerBackfillService.lookupNumbered(blocks, 3)).isEqualTo("z");
        assertThat(AiAnswerBackfillService.lookupNumbered(blocks, 9)).isNull();
    }
}
