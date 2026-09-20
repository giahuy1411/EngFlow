package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.ExerciseAttemptRepository;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ExerciseStudyActivityTest {
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private ExerciseAttemptRepository attemptRepository;
    @Mock private UserRepository userRepository;
    @Mock private LessonContentService lessonContentService;
    @Mock private StudyActivityService studyActivityService;
    @InjectMocks private ExerciseService service;

    @BeforeEach
    void setUp() {
        when(userRepository.findByEmail("study@example.test"))
                .thenReturn(Optional.of(User.builder().id(42L).build()));
        when(exerciseRepository.findByLessonIdOrderByOrderIndexAsc(7L))
                .thenReturn(List.of(Exercise.builder().id(1L).correctAnswer("right").build()));
    }

    private GradeRequest answers(Long exerciseId, String answer) {
        GradeRequest request = new GradeRequest();
        GradeRequest.AnswerItem item = new GradeRequest.AnswerItem();
        item.setExerciseId(exerciseId);
        item.setUserAnswer(answer);
        request.setAnswers(List.of(item));
        return request;
    }

    @Test
    void wrongButNonemptyAnswerCountsOnlyAfterAttemptSaved() {
        service.submitExercises(7L, answers(1L, "wrong"), "study@example.test");
        var order = inOrder(attemptRepository, studyActivityService);
        order.verify(attemptRepository).save(any());
        order.verify(studyActivityService).recordStudy(42L);
    }

    @Test
    void blankAnswerDoesNotCount() {
        service.submitExercises(7L, answers(1L, "  "), "study@example.test");
        verifyNoInteractions(studyActivityService);
    }

    @Test
    void foreignExerciseDoesNotCount() {
        service.submitExercises(7L, answers(888L, "answer"), "study@example.test");
        verifyNoInteractions(studyActivityService);
    }

    @Test
    void persistenceFailureDoesNotCount() {
        when(attemptRepository.save(any())).thenThrow(new IllegalStateException("save failed"));
        assertThatThrownBy(() -> service.submitExercises(7L, answers(1L, "answer"), "study@example.test"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(studyActivityService);
    }
}
