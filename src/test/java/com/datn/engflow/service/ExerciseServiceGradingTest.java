package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.dto.response.ExerciseGradeItem;
import com.datn.engflow.model.dto.response.GradeResponse;
import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.repository.ExerciseAttemptRepository;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Regression tests for the grading contract: an empty answer key can never
 * verify a response (no more null-vs-empty false positives).
 */
@ExtendWith(MockitoExtension.class)
class ExerciseServiceGradingTest {

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

    @InjectMocks
    private ExerciseService exerciseService;

    private Exercise keyedExercise;
    private Exercise unkeyedExercise;

    @BeforeEach
    void setUp() {
        keyedExercise = Exercise.builder()
                .id(1L)
                .question("My dad …… maths at university.")
                .correctAnswer("studied")
                .exerciseType(ExerciseType.FILL_BLANK)
                .build();
        unkeyedExercise = Exercise.builder()
                .id(2L)
                .question("Tom …… (get) a big surprise")
                .correctAnswer("")
                .exerciseType(ExerciseType.FILL_BLANK)
                .build();
    }

    private GradeRequest requestOf(GradeRequest.AnswerItem... items) {
        GradeRequest request = new GradeRequest();
        request.setAnswers(new java.util.ArrayList<>(List.of(items)));
        return request;
    }

    private GradeRequest.AnswerItem answer(Long id, String value) {
        GradeRequest.AnswerItem item = new GradeRequest.AnswerItem();
        item.setExerciseId(id);
        item.setUserAnswer(value);
        return item;
    }

    private void mockExercises(Exercise... exercises) {
        Lesson lesson = Lesson.builder().id(567L).title("Past simple").build();
        for (Exercise ex : exercises) {
            lenient().when(exerciseRepository.findByLessonIdOrderByOrderIndexAsc(567L))
                    .thenReturn(new java.util.ArrayList<>(List.of(exercises)));
        }
        if (exercises.length == 0) {
            lenient().when(exerciseRepository.findByLessonIdOrderByOrderIndexAsc(567L))
                    .thenReturn(new java.util.ArrayList<>());
        }
    }

    @Test
    @DisplayName("Grade: đúng đáp án → correct=true, điểm tính vào gradeable")
    void gradesCorrectAnswer() {
        mockExercises(keyedExercise, unkeyedExercise);
        GradeResponse res = exerciseService.gradeExercises(567L, requestOf(answer(1L, "Studied ")));

        assertThat(res.getScore()).isEqualTo(1);
        assertThat(res.getTotal()).isEqualTo(1);
        assertThat(res.getPercentage()).isEqualTo(100.0);
        assertThat(res.getResults()).hasSize(1);
        assertThat(res.getResults().get(0).isCorrect()).isTrue();
        assertThat(res.getResults().get(0).isUngradeable()).isFalse();
    }

    @Test
    @DisplayName("Grade: sai đáp án → correct=false")
    void gradesWrongAnswer() {
        mockExercises(keyedExercise);
        GradeResponse res = exerciseService.gradeExercises(567L, requestOf(answer(1L, "moved")));

        assertThat(res.getScore()).isZero();
        assertThat(res.getResults().get(0).isCorrect()).isFalse();
        assertThat(res.getResults().get(0).isUngradeable()).isFalse();
    }

    @Test
    @DisplayName("Grade regression: answer key rỗng → ungradeable, KHÔNG correct=true")
    void emptyKeyIsUngradeableNotCorrect() {
        mockExercises(unkeyedExercise);
        // Trường hợp gây false-positive cũ: null userAnswer so với key "".
        GradeResponse resNull = exerciseService.gradeExercises(567L,
                requestOf(answer(2L, null)));
        assertThat(resNull.getResults().get(0).isCorrect()).isFalse();
        assertThat(resNull.getResults().get(0).isUngradeable()).isTrue();
        assertThat(resNull.getScore()).isZero();
        assertThat(resNull.getTotal()).isZero();

        // Cả khi userAnswer chuỗi rỗng.
        GradeResponse resEmpty = exerciseService.gradeExercises(567L,
                requestOf(answer(2L, "")));
        assertThat(resEmpty.getResults().get(0).isCorrect()).isFalse();
        assertThat(resEmpty.getResults().get(0).isUngradeable()).isTrue();
        assertThat(resEmpty.getScore()).isZero();
        assertThat(resEmpty.getTotal()).isZero();
    }

    @Test
    @DisplayName("Grade: mixed keyed + unkeyed → điểm chia cho số câu gradeable")
    void mixedKeyedUnkeyedDenominator() {
        mockExercises(keyedExercise, unkeyedExercise);
        GradeResponse res = exerciseService.gradeExercises(567L,
                requestOf(answer(1L, "studied"), answer(2L, "anything")));

        assertThat(res.getResults()).hasSize(2);
        assertThat(res.getResults().get(0).isCorrect()).isTrue();
        assertThat(res.getResults().get(1).isUngradeable()).isTrue();
        assertThat(res.getScore()).isEqualTo(1);
        assertThat(res.getTotal()).isEqualTo(1);            // denominator = gradeable only
        assertThat(res.getPercentage()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Grade: null answer key (không chỉ '') cũng ungradeable")
    void nullKeyIsUngradeable() {
        Exercise nullKey = Exercise.builder()
                .id(3L)
                .question("gap")
                .correctAnswer(null)
                .exerciseType(ExerciseType.FILL_BLANK)
                .build();
        mockExercises(nullKey);
        GradeResponse res = exerciseService.gradeExercises(567L, requestOf(answer(3L, "get")));

        assertThat(res.getResults().get(0).isUngradeable()).isTrue();
        assertThat(res.getResults().get(0).isCorrect()).isFalse();
        assertThat(res.getTotal()).isZero();
    }

    @Test
    @DisplayName("Grade: answers null → empty response, không NPE")
    void nullAnswersReturnsEmpty() {
        GradeResponse res = exerciseService.gradeExercises(567L, new GradeRequest());
        assertThat(res.getResults()).isEmpty();
        assertThat(res.getTotal()).isZero();
    }

    @Test
    @DisplayName("Submit: lưu attempt với score/total chuẩn kể cả khi một số ungradeable")
    void submitSavesAttemptWhenGradeable() {
        mockExercises(keyedExercise);
        com.datn.engflow.model.entity.User user = com.datn.engflow.model.entity.User.builder()
                .id(9L)
                .email("user@gmail.com")
                .build();
        lenient().when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));
        lenient().when(exerciseRepository.findAllById(anyList())).thenReturn(List.of(keyedExercise));
        lenient().when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GradeResponse res = exerciseService.submitExercises(567L,
                requestOf(answer(1L, "studied")), "user@gmail.com");

        assertThat(res.getScore()).isEqualTo(1);
        assertThat(res.getTotal()).isEqualTo(1);
        verify(attemptRepository).save(any(com.datn.engflow.model.entity.ExerciseAttempt.class));
    }
}
