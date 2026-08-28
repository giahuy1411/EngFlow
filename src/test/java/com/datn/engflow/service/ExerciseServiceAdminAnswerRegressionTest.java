package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.ExerciseRequest;
import com.datn.engflow.model.dto.response.ExerciseResponse;
import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.repository.ExerciseAttemptRepository;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Regression tests for F7-BUG01: admin-facing exercise endpoints must return
 * {@code correctAnswer} and {@code explanation} so the admin UI can display and
 * re-edit them. Student-facing paths must continue to strip these fields.
 */
@ExtendWith(MockitoExtension.class)
class ExerciseServiceAdminAnswerRegressionTest {

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

    private Exercise sampleExercise() {
        Lesson lesson = Lesson.builder().id(444L).title("Grammar 101").build();
        return Exercise.builder()
                .id(1L)
                .lesson(lesson)
                .question("What is 6 * 7?")
                .options("[\"42\",\"48\",\"36\"]")
                .correctAnswer("42")
                .explanation("6 times 7 equals 42.")
                .exerciseType(ExerciseType.MULTIPLE_CHOICE)
                .difficulty(ExerciseDifficulty.EASY)
                .orderIndex(1)
                .build();
    }

    @Test
    void createExerciseReturnsCorrectAnswerToAdmin() {
        Lesson lesson = Lesson.builder().id(444L).build();
        when(lessonRepository.findById(444L)).thenReturn(Optional.of(lesson));
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));

        ExerciseRequest request = new ExerciseRequest();
        request.setLessonId(444L);
        request.setQuestion("What is 6 * 7?");
        request.setOptions("[\"42\",\"48\",\"36\"]");
        request.setCorrectAnswer("42");
        request.setExplanation("6 times 7 equals 42.");
        request.setExerciseType("MULTIPLE_CHOICE");
        request.setDifficulty("EASY");
        request.setOrderIndex(1);

        ExerciseResponse response = exerciseService.createExercise(request);

        assertThat(response.getCorrectAnswer()).isEqualTo("42");
        assertThat(response.getExplanation()).isEqualTo("6 times 7 equals 42.");
    }

    @Test
    void getExerciseReturnsCorrectAnswerToAdmin() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(sampleExercise()));

        ExerciseResponse response = exerciseService.getExercise(1L);

        assertThat(response.getCorrectAnswer()).isEqualTo("42");
        assertThat(response.getExplanation()).isEqualTo("6 times 7 equals 42.");
    }

    @Test
    void updateExerciseReturnsCorrectAnswerToAdmin() {
        Exercise existing = sampleExercise();
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(exerciseRepository.save(any(Exercise.class))).thenAnswer(inv -> inv.getArgument(0));

        ExerciseRequest request = new ExerciseRequest();
        request.setLessonId(444L);
        request.setQuestion("What is 6 * 7?");
        request.setCorrectAnswer("42");
        request.setExerciseType("MULTIPLE_CHOICE");

        ExerciseResponse response = exerciseService.updateExercise(1L, request);

        assertThat(response.getCorrectAnswer()).isEqualTo("42");
    }

    @Test
    void createExerciseThrowsWhenLessonMissing() {
        when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

        ExerciseRequest request = new ExerciseRequest();
        request.setLessonId(999L);
        request.setQuestion("q");
        request.setExerciseType("MULTIPLE_CHOICE");

        assertThatThrownBy(() -> exerciseService.createExercise(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Lesson not found");
    }
}
