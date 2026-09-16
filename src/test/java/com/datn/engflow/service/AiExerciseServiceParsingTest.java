package com.datn.engflow.service;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AiExerciseService JSON salvage parsing and schema validation,
 * ported from the engflow-language-mcp reference implementation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiExerciseServiceParsingTest {

    @Mock
    private com.datn.engflow.repository.ExerciseRepository exerciseRepository;

    @Mock
    private com.datn.engflow.repository.LessonRepository lessonRepository;

    private AiExerciseService service;

    @BeforeEach
    void setUp() {
        service = new AiExerciseService(
                new ObjectMapper(),
                null, // research service not exercised here
                null, // tts not exercised here
                null, // cloudinary not exercised here
                exerciseRepository,
                lessonRepository);
    }

    // ── parseExerciseJsonArray: layer 1 (direct parse) ──

    @Test
    @DisplayName("parseExerciseJsonArray: parses a clean JSON array")
    void parsesCleanArray() {
        String response = """
                [{"question":"The sun ___ in the east.","options":["rise","rises"],"correctAnswer":"rises","explanation":"3rd person","difficulty":"EASY"}]
                """;
        List<Map<String, Object>> result = AiExerciseService.parseExerciseJsonArray(response);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("correctAnswer", "rises");
    }

    @Test
    @DisplayName("parseExerciseJsonArray: strips ```json fences (layer 1)")
    void parsesFencedArray() {
        String response = """
                ```json
                [{"question":"Water ___ at 100C.","correctAnswer":"boils","difficulty":"EASY"}]
                ```
                """;
        List<Map<String, Object>> result = AiExerciseService.parseExerciseJsonArray(response);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("correctAnswer", "boils");
    }

    // ── layer 2 (regex array extraction + truncation auto-close) ──

    @Test
    @DisplayName("parseExerciseJsonArray: extracts array wrapped in prose (layer 2)")
    void extractsArrayFromProse() {
        String response = """
                Here are your exercises:
                [{"question":"Tom ___ and Betty ___ in my class.","correctAnswer":"are","difficulty":"EASY"}]
                Hope this helps!
                """;
        List<Map<String, Object>> result = AiExerciseService.parseExerciseJsonArray(response);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("correctAnswer", "are");
    }

    @Test
    @DisplayName("parseExerciseJsonArray: truncation mid-object still salvages complete objects")
    void autoClosesTruncatedArray() {
        String response = """
                [{"question":"She ___ a teacher.","correctAnswer":"is","difficulty":"EASY"},
                 {"question":"I ___ seventeen years old.","correctAnswer":"am"
                """;
        List<Map<String, Object>> result = AiExerciseService.parseExerciseJsonArray(response);
        // The second object is cut mid-way and cannot be recovered, but the first
        // complete object MUST be salvaged instead of failing the whole batch.
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("correctAnswer", "is");
    }

    // ── layer 3 (object salvage) ──

    @Test
    @DisplayName("parseExerciseJsonArray: salvages complete objects from broken array (layer 3)")
    void salvagesObjectsFromBrokenArray() {
        String response = """
                [{"question":"My pencil ___ in my bag.","correctAnswer":"is","difficulty":"EASY"},
                 {"question":"We ___ at home","correctAnswer":"are" INVALID SYNTAX HERE
                """;
        List<Map<String, Object>> result = AiExerciseService.parseExerciseJsonArray(response);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("correctAnswer", "is");
    }

    @Test
    @DisplayName("parseExerciseJsonArray: returns null for garbage input")
    void returnsNullForGarbage() {
        assertThat(AiExerciseService.parseExerciseJsonArray(null)).isNull();
        assertThat(AiExerciseService.parseExerciseJsonArray("")).isNull();
        assertThat(AiExerciseService.parseExerciseJsonArray("Sorry, I cannot generate exercises.")).isNull();
    }

    // ── validateSchema: MATCHING format contract with frontend ──

    @Test
    @DisplayName("validateSchema: accepts MATCHING with left|right options and l=r answer")
    void acceptsValidMatching() throws Exception {
        Exercise ex = Exercise.builder()
                .exerciseType(ExerciseType.MATCHING)
                .question("Match similar words:")
                .options(new ObjectMapper().writeValueAsString(
                        List.of("big|large", "small|tiny", "fast|quick", "smart|clever")))
                .correctAnswer("big=large,small=tiny,fast=quick,smart=clever")
                .build();
        assertThat(service.validateSchema(ex)).isNull();
    }

    @Test
    @DisplayName("validateSchema: rejects MATCHING options without pipe separator")
    void rejectsMatchingWithoutPipes() throws Exception {
        Exercise ex = Exercise.builder()
                .exerciseType(ExerciseType.MATCHING)
                .question("Match:")
                .options(new ObjectMapper().writeValueAsString(List.of("is", "am", "are")))
                .correctAnswer("is=am")
                .build();
        assertThat(service.validateSchema(ex)).contains("left|right");
    }

    @Test
    @DisplayName("validateSchema: rejects MATCHING answer without = pairs")
    void rejectsMatchingAnswerWithoutEquals() throws Exception {
        Exercise ex = Exercise.builder()
                .exerciseType(ExerciseType.MATCHING)
                .question("Match:")
                .options(new ObjectMapper().writeValueAsString(List.of("big|large", "small|tiny")))
                .correctAnswer("big large, small tiny")
                .build();
        assertThat(service.validateSchema(ex)).contains("left=right");
    }

    // ── buildPrompt: few-shot example lock ──

    @Test
    @DisplayName("buildPrompt: includes left|right few-shot example for MATCHING")
    void matchingPromptHasFewShotExample() {
        com.datn.engflow.model.entity.Lesson lesson = new com.datn.engflow.model.entity.Lesson();
        lesson.setTitle("Adjectives");
        lesson.setContent("big small fast smart");
        String prompt = service.buildPrompt(lesson, ExerciseType.MATCHING, 3, null);
        assertThat(prompt).contains("big|large");
        assertThat(prompt).contains("big=large");
        assertThat(prompt).doesNotContain(":::");
        // Reminder comes after the lesson content (attention-decay countermeasure)
        assertThat(prompt.indexOf("big|large")).isLessThan(prompt.indexOf("Content:"));
        assertThat(prompt).endsWith(".");
        assertThat(prompt).contains("REMINDER");
        assertThat(prompt).contains("NEVER whole-sentence options");
    }

    // ── dedup helpers ──

    @Test
    @DisplayName("parseExerciseJsonArray: salvages prefixed objects without array wrapper (real LLM shape)")
    void salvagesPrefixedObjectsWithoutArray() {
        // Actual qwen2.5:1.5b output observed at runtime: each object is prefixed
        // with a "MATCHING: " label and no [ ] array wrapper is produced.
        String response = """
                MATCHING: {"question":"Match similar verbs to complete the sentences.","options":["agree|say","love|like","enjoy|liking","believe|trust"],"correctAnswer":"agree=say,love=like,enjoy=liking,believe=trust","explanation":"Matching verbs that have similar meanings.","difficulty":"EASY"}

                MATCHING: {"question":"Match similar verbs with their meanings.","options":["perform|execute","conduct|control","execute|perform","manage|control"],"correctAnswer":"perform=execute,conduct=control,execute=perform,manage=control","explanation":"Matching verbs with their synonyms.","difficulty":"MEDIUM"}
                """;
        List<Map<String, Object>> result = AiExerciseService.parseExerciseJsonArray(response);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("options")).isNotNull();
    }

    @Test
    @DisplayName("questionHash: same question in different case/whitespace hashes equal")
    void questionHashNormalizes() {
        Exercise a = Exercise.builder().question("  The Sun Rises  ").build();
        Exercise b = Exercise.builder().question("the sun rises").build();
        assertThat(AiExerciseService.questionHashForTest(a))
                .isEqualTo(AiExerciseService.questionHashForTest(b));
    }

    // ── example-copy guard ──

    @Test
    @DisplayName("isExampleCopy: rejects verbatim few-shot example question")
    void rejectsVerbatimExample() {
        Exercise copy = Exercise.builder()
                .exerciseType(ExerciseType.MATCHING)
                .question("Match similar words:")
                .options("[\"big|large\",\"small|tiny\",\"fast|quick\",\"smart|clever\"]")
                .correctAnswer("big=large,small=tiny,fast=quick,smart=clever")
                .build();
        assertThat(AiExerciseService.isExampleCopyForTest(copy)).isTrue();
    }

    @Test
    @DisplayName("isExampleCopy: accepts genuinely new question")
    void acceptsNewQuestion() {
        Exercise fresh = Exercise.builder()
                .exerciseType(ExerciseType.MATCHING)
                .question("Match each verb with its past tense:")
                .options("[\"go|went\",\"eat|ate\",\"see|saw\",\"take|took\"]")
                .correctAnswer("go=went,eat=ate,see=saw,take=took")
                .build();
        assertThat(AiExerciseService.isExampleCopyForTest(fresh)).isFalse();
    }

    // ── MATCHING slash-pair repair ──

    @Test
    @DisplayName("repairMatchingSlashPairs: converts 'left / right' drift to left|right")
    void repairsSlashPairs() throws Exception {
        Exercise drifted = Exercise.builder()
                .exerciseType(ExerciseType.MATCHING)
                .question("Complete the sentences with the verb be.")
                .options(new ObjectMapper().writeValueAsString(
                        List.of("The book / is on the desk", "They / are in the room",
                                "The cat / is sleeping", "He / is tired")))
                .correctAnswer("garbage from the model")
                .explanation("Verb be.")
                .difficulty(ExerciseDifficulty.EASY)
                .build();
        Exercise repaired = service.repairMatchingSlashPairs(drifted);
        assertThat(repaired).isNotNull();
        assertThat(service.validateSchema(repaired)).isNull();
        assertThat(repaired.getOptions()).contains("The book|is on the desk");
        assertThat(repaired.getCorrectAnswer()).contains("The book=is on the desk");
        assertThat(repaired.getCorrectAnswer()).hasSize("The book=is on the desk,They=are in the room,The cat=is sleeping,He=is tired".length());
    }

    @Test
    @DisplayName("repairMatchingSlashPairs: rejects inconsistent or mixed shapes")
    void refusesBadShapes() throws Exception {
        // Mixed separators — not repairable
        Exercise mixed = Exercise.builder()
                .exerciseType(ExerciseType.MATCHING)
                .question("q")
                .options(new ObjectMapper().writeValueAsString(
                        List.of("The book / is on the desk", "big|large")))
                .correctAnswer("x")
                .build();
        assertThat(service.repairMatchingSlashPairs(mixed)).isNull();
        // Already valid — nothing to repair (returns null, caller keeps original)
        Exercise valid = Exercise.builder()
                .exerciseType(ExerciseType.MATCHING)
                .question("q")
                .options(new ObjectMapper().writeValueAsString(List.of("big|large", "small|tiny")))
                .correctAnswer("big=large,small=tiny")
                .build();
        assertThat(service.repairMatchingSlashPairs(valid)).isNull();
    }

    // ── frontend contract: options JSON round-trip through MatchingExercise.vue ──

    @Test
    @DisplayName("MATCHING options serialization matches frontend parse contract")
    void matchingOptionsRoundTrip() throws Exception {
        // What Java persists (options as JSON string of "left|right" pairs) must be
        // exactly what MatchingExercise.vue onMounted() parses via opt.includes('|').
        Exercise ex = Exercise.builder()
                .exerciseType(ExerciseType.MATCHING)
                .question("Match the opposites:")
                .options(new ObjectMapper().writeValueAsString(
                        List.of("hot|cold", "big|small", "fast|slow", "happy|sad")))
                .correctAnswer("hot=cold,big=small,fast=slow,happy=sad")
                .build();
        assertThat(service.validateSchema(ex)).isNull();
        List<?> persisted = new ObjectMapper().readValue(ex.getOptions(), List.class);
        assertThat(persisted).allMatch(o -> ((String) o).contains("|"));
        // correctAnswer pairs must correspond 1:1 with options pairs
        String[] pairs = ex.getCorrectAnswer().split(",");
        assertThat(persisted).hasSameSizeAs(pairs);
    }

    // ── audit-v8: MULTIPLE_CHOICE option quality ──

    @Test
    @DisplayName("validateSchema: rejects MULTIPLE_CHOICE with duplicate options")
    void rejectsDuplicateOptions() throws Exception {
        // measured model failure mode: ["most expensive","more expensive","best","best"]
        Exercise ex = Exercise.builder()
                .exerciseType(ExerciseType.MULTIPLE_CHOICE)
                .question("What is the superlative of expensive?")
                .options(new ObjectMapper().writeValueAsString(
                        List.of("most expensive", "more expensive", "best", "best")))
                .correctAnswer("best")
                .build();
        assertThat(service.validateSchema(ex)).contains("distinct");
    }

    @Test
    @DisplayName("validateSchema: duplicate check ignores case and surrounding space")
    void rejectsDuplicateOptionsIgnoringCase() throws Exception {
        Exercise ex = Exercise.builder()
                .exerciseType(ExerciseType.MULTIPLE_CHOICE)
                .question("Pick one:")
                .options(new ObjectMapper().writeValueAsString(List.of(" have ", "HAS", "has")))
                .correctAnswer("has")
                .build();
        assertThat(service.validateSchema(ex)).contains("distinct");
    }

    @Test
    @DisplayName("validateSchema: distinct options still accepted")
    void acceptsDistinctOptions() throws Exception {
        Exercise ex = Exercise.builder()
                .exerciseType(ExerciseType.MULTIPLE_CHOICE)
                .question("Pick one:")
                .options(new ObjectMapper().writeValueAsString(List.of("have", "has", "had", "having")))
                .correctAnswer("has")
                .build();
        assertThat(service.validateSchema(ex)).isNull();
    }
}
