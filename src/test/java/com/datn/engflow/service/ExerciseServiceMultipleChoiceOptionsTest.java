package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.model.dto.request.ExerciseRequest;
import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.repository.ExerciseAttemptRepository;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Regression tests for audit-v13 F-13-01, revised after adversarial review.
 *
 * <p>The guard must reject a NEW multiple-choice item that carries no usable answer
 * text, while NOT blocking:
 * <ul>
 *   <li>legitimate multiple-choice items with real options,</li>
 *   <li>edits of the 32,814 legacy {@code MULTIPLE_CHOICE} rows whose options are NULL
 *       (measured in the live DB on 2026-09-22: 32,814 of 33,556 MC rows have
 *       {@code options IS NULL}, and 0 have bare-letter options),</li>
 *   <li>FILL_BLANK / other types.</li>
 * </ul>
 *
 * <p>Note: these are unit tests with mocked repositories. They do not by themselves
 * prove end-to-end behaviour — that was verified against the live container (see
 * {@code .specify/specs/audit-v13-full/evidence/f13-01-api-verify.json}).
 */
@ExtendWith(MockitoExtension.class)
class ExerciseServiceMultipleChoiceOptionsTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ExerciseAttemptRepository attemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonContentService lessonContentService;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private ExerciseService exerciseService;

    @BeforeEach
    void wireObjectMapper() throws Exception {
        org.mockito.Mockito.lenient()
                .when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenAnswer(inv -> new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(inv.getArgument(0, String.class), inv.getArgument(1, com.fasterxml.jackson.core.type.TypeReference.class)));
    }

    private ExerciseRequest request(String exerciseType, String options) {
        ExerciseRequest r = new ExerciseRequest();
        r.setLessonId(91920L);
        r.setQuestion("câu 1");
        r.setExerciseType(exerciseType);
        r.setOptions(options);
        r.setCorrectAnswer("d");
        return r;
    }

    private void lessonExists() {
        when(lessonRepository.findById(91920L))
                .thenReturn(Optional.of(Lesson.builder().id(91920L).title("thầy bình").build()));
    }

    // --- create: rejects unusable new choice items ---

    @Test
    void createRejectsMultipleChoiceWithBareLetterOptions() {
        lessonExists();
        assertThatThrownBy(() -> exerciseService.createExercise(
                request("MULTIPLE_CHOICE", "[\"a\",\"b\",\"c\",\"d\"]")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("MULTIPLE_CHOICE");
    }

    @Test
    void createRejectsMultipleChoiceWithUpperCaseLetterOptions() {
        lessonExists();
        assertThatThrownBy(() -> exerciseService.createExercise(
                request("MULTIPLE_CHOICE", "[\"A\",\"B\",\"C\",\"D\"]")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createRejectsMultipleChoiceWithFewerThanTwoRealOptions() {
        lessonExists();
        assertThatThrownBy(() -> exerciseService.createExercise(
                request("MULTIPLE_CHOICE", "[\"only one\"]")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createRejectsMultipleChoiceWithNoOptionsAtAll() {
        lessonExists();
        assertThatThrownBy(() -> exerciseService.createExercise(
                request("MULTIPLE_CHOICE", null)))
                .isInstanceOf(BadRequestException.class);
    }

    // --- create: accepts everything legitimate ---

    @Test
    void createAcceptsMultipleChoiceWithRealOptionText() {
        lessonExists();
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThatCode(() -> exerciseService.createExercise(
                request("MULTIPLE_CHOICE", "[\"rise\",\"rises\",\"rose\",\"rising\"]")))
                .doesNotThrowAnyException();
    }

    @Test
    void createAcceptsMultipleChoiceWithMixedLettersAndRealText() {
        lessonExists();
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));
        // Contains usable content, so it is allowed.
        assertThatCode(() -> exerciseService.createExercise(
                request("MULTIPLE_CHOICE", "[\"a\",\"b\",\"c\",\"Hanoi\"]")))
                .doesNotThrowAnyException();
    }

    @Test
    void createAcceptsMultipleChoiceWithLetterDashContentOptions() {
        lessonExists();
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));
        // Real row 651717: options ["A - Salad","B - Cheeseburger",...]. "A - Salad"
        // carries real answer content and must NOT be treated as a placeholder.
        assertThatCode(() -> exerciseService.createExercise(
                request("MULTIPLE_CHOICE",
                        "[\"A - Salad\", \"B - Cheeseburger\", \"C - Pizza\", \"D - Bread\"]")))
                .doesNotThrowAnyException();
    }

    @Test
    void createStillAcceptsFillBlankWithoutOptions() {
        lessonExists();
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThatCode(() -> exerciseService.createExercise(request("FILL_BLANK", null)))
                .doesNotThrowAnyException();
    }

    @Test
    void createAcceptsLowercaseExerciseType() {
        lessonExists();
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));
        // case-insensitive check, and valueOf accepts the canonical name
        assertThatCode(() -> exerciseService.createExercise(
                request("MULTIPLE_CHOICE", "[\"go\",\"goes\"]")))
                .doesNotThrowAnyException();
    }

    // --- update: must NOT block edits of legacy NULL-option rows ---

    @Test
    void updateAllowsEditingQuestionOfLegacyMultipleChoiceRowWithNullOptions() {
        Lesson lesson = Lesson.builder().id(91920L).title("thầy bình").build();
        Exercise existing = Exercise.builder().id(766031L).lesson(lesson)
                .question("old").options(null).correctAnswer("Those are our sisters.")
                .exerciseType(ExerciseType.MULTIPLE_CHOICE).build();
        when(exerciseRepository.findById(766031L)).thenReturn(Optional.of(existing));
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));

        ExerciseRequest req = new ExerciseRequest();
        req.setQuestion("fixed typo");   // options intentionally omitted
        assertThatCode(() -> exerciseService.updateExercise(766031L, req))
                .doesNotThrowAnyException();
    }

    @Test
    void updateRejectsSwitchingToMultipleChoiceWithBareLetterOptions() {
        Lesson lesson = Lesson.builder().id(91920L).title("thầy bình").build();
        Exercise existing = Exercise.builder().id(777434L).lesson(lesson).question("câu2").build();
        when(exerciseRepository.findById(777434L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> exerciseService.updateExercise(777434L,
                request("MULTIPLE_CHOICE", "[\"a\",\"b\",\"c\",\"d\"]")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateRejectsSupplyingUnusableOptionsForAChoiceRow() {
        Lesson lesson = Lesson.builder().id(91920L).title("thầy bình").build();
        Exercise existing = Exercise.builder().id(777434L).lesson(lesson).question("câu2")
                .exerciseType(ExerciseType.MULTIPLE_CHOICE).build();
        when(exerciseRepository.findById(777434L)).thenReturn(Optional.of(existing));

        // options supplied but unusable -> reject
        ExerciseRequest req = new ExerciseRequest();
        req.setOptions("[\"a\",\"b\"]");
        assertThatThrownBy(() -> exerciseService.updateExercise(777434L, req))
                .isInstanceOf(BadRequestException.class);
    }
}
