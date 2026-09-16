package com.datn.engflow.service;

import com.datn.engflow.model.dto.projection.LessonTitle;
import com.datn.engflow.model.dto.response.ExerciseResponse;
import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.repository.ExerciseAttemptRepository;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceAdminPaginationTest {

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

    private static LessonTitle title(Long id, String t) {
        return new LessonTitle() {
            @Override public Long getLessonId() { return id; }
            @Override public String getTitle() { return t; }
        };
    }

    @Test
    void adminPageForwardsAllFiltersToRepository() {
        Pageable pageable = PageRequest.of(2, 20);
        Lesson lesson = Lesson.builder().id(5L).title("Grammar 101").build();
        Exercise exercise = Exercise.builder()
                .id(1L)
                .lesson(lesson)
                .question("She ___ to school.")
                .correctAnswer("goes")
                .exerciseType(ExerciseType.MULTIPLE_CHOICE)
                .difficulty(ExerciseDifficulty.EASY)
                .build();
        Page<Exercise> page = new PageImpl<>(List.of(exercise), pageable, 1);

        when(exerciseRepository.findAdminPage(
                5L, ExerciseType.MULTIPLE_CHOICE, ExerciseDifficulty.EASY, "school", pageable))
                .thenReturn(page);
        // audit-v8 perf: the row label now comes from one batched id+title query instead
        // of hydrating the joined Lesson (which dragged content/content_original along).
        when(lessonRepository.findTitlesById(Set.of(5L))).thenReturn(List.of(title(5L, "Grammar 101")));

        Page<ExerciseResponse> result = exerciseService.getAdminExercisePage(
                5L, "multiple_choice", "easy", "  school  ", pageable);

        assertThat(result.getContent()).hasSize(1);
        ExerciseResponse dto = result.getContent().get(0);
        assertThat(dto.getLessonId()).isEqualTo(5L);
        assertThat(dto.getLessonTitle()).isEqualTo("Grammar 101");
        assertThat(dto.getExerciseType()).isEqualTo("MULTIPLE_CHOICE");
        assertThat(dto.getDifficulty()).isEqualTo("EASY");
        // includeAnswers=true -> answer and explanation visible for admin
        assertThat(dto.getCorrectAnswer()).isEqualTo("goes");
        verify(exerciseRepository)
                .findAdminPage(5L, ExerciseType.MULTIPLE_CHOICE, ExerciseDifficulty.EASY, "school", pageable);
        verify(lessonRepository).findTitlesById(Set.of(5L));
    }

    @Test
    void adminPageSkipsTitleLookupWhenPageIsEmpty() {
        Pageable pageable = PageRequest.of(0, 20);
        when(exerciseRepository.findAdminPage(null, null, null, null, pageable))
                .thenReturn(Page.empty(pageable));

        Page<ExerciseResponse> result =
                exerciseService.getAdminExercisePage(null, "  ", "", null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(exerciseRepository).findAdminPage(null, null, null, null, pageable);
        // no ids on the page -> no second query at all
        verify(lessonRepository, org.mockito.Mockito.never()).findTitlesById(org.mockito.ArgumentMatchers.anySet());
    }

    @Test
    void pageMetadataPreservedThroughMapping() {
        Pageable pageable = PageRequest.of(0, 20);
        when(exerciseRepository.findAdminPage(null, null, null, null, pageable))
                .thenReturn(Page.empty(pageable));

        Page<ExerciseResponse> result =
                exerciseService.getAdminExercisePage(null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(20);
    }
}
