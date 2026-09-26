package com.datn.engflow.service;

import com.datn.engflow.model.dto.projection.LessonListProjection;
import com.datn.engflow.model.dto.response.LessonListItemResponse;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.Progress;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.ExerciseAttemptRepository;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.LessonSubmissionRepository;
import com.datn.engflow.repository.ProgressRepository;
import com.datn.engflow.repository.SpeakingPromptRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServicePaginationTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProgressRepository progressRepository;

    @Mock
    private ExerciseAttemptRepository exerciseAttemptRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private LessonSubmissionRepository lessonSubmissionRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private SpeakingPromptRepository speakingPromptRepository;

    @InjectMocks
    private LessonService lessonService;

    private Lesson lesson(Long id, String title) {
        return Lesson.builder()
                .id(id)
                .title(title)
                .description("desc " + id)
                .level(LessonLevel.ELEMENTARY)
                .category("GRAMMAR")
                .durationMinutes(30)
                .orderIndex(1)
                .build();
    }

    /** Build a lightweight projection mock from a Lesson. */
    private LessonListProjection projection(Lesson l) {
        LessonListProjection p = org.mockito.Mockito.mock(LessonListProjection.class);
        org.mockito.Mockito.when(p.getId()).thenReturn(l.getId());
        org.mockito.Mockito.when(p.getTitle()).thenReturn(l.getTitle());
        org.mockito.Mockito.when(p.getDescription()).thenReturn(l.getDescription());
        org.mockito.Mockito.when(p.getLevel()).thenReturn(l.getLevel());
        org.mockito.Mockito.when(p.getCategory()).thenReturn(l.getCategory());
        org.mockito.Mockito.when(p.getDurationMinutes()).thenReturn(l.getDurationMinutes());
        org.mockito.Mockito.when(p.getThumbnailUrl()).thenReturn(l.getThumbnailUrl());
        org.mockito.Mockito.when(p.getAudioUrl()).thenReturn(l.getAudioUrl());
        org.mockito.Mockito.when(p.getSkillType()).thenReturn(l.getSkillType());
        org.mockito.Mockito.when(p.getOrderIndex()).thenReturn(l.getOrderIndex());
        return p;
    }

    @Test
    void publishedPageDoesNotExposeHeavyContentOrVocabularies() {
        Pageable pageable = PageRequest.of(0, 12);
        Lesson lesson = lesson(1L, "Intro to English");
        Page<LessonListProjection> page = new PageImpl<>(List.of(projection(lesson)), pageable, 1);
        when(lessonRepository.findPublishedPageProjection("intro", null, pageable)).thenReturn(page);

        Page<LessonListItemResponse> result =
                lessonService.getPublishedLessonPage(null, "  intro  ", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        LessonListItemResponse dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Intro to English");
        assertThat(dto.getCompletionPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        // Lightweight DTO: no content / vocabularies fields by design
        assertThat(dto.getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("content", "vocabularies");
        verify(lessonRepository).findPublishedPageProjection("intro", null, pageable);
    }

    @Test
    void userProgressAppliedOnlyToLessonsOnCurrentPage() {
        Pageable pageable = PageRequest.of(0, 12);
        Lesson l1 = lesson(1L, "Lesson One");
        Lesson l2 = lesson(2L, "Lesson Two");
        Page<LessonListProjection> page = new PageImpl<>(List.of(projection(l1), projection(l2)), pageable, 2);

        User user = User.builder().id(99L).email("user@gmail.com").build();
        Progress done = Progress.builder()
                .lesson(l1)
                .isCompleted(true)
                .completionPercentage(new BigDecimal("75.00"))
                .build();

        when(lessonRepository.findPublishedPageProjection(null, null, pageable)).thenReturn(page);
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));
        when(progressRepository.findByUserIdAndLessonIdIn(eq(99L), anyCollection()))
                .thenReturn(List.of(done));

        Page<LessonListItemResponse> result =
                lessonService.getPublishedLessonPage("user@gmail.com", null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getIsCompleted()).isTrue();
        assertThat(result.getContent().get(0).getCompletionPercentage())
                .isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(result.getContent().get(1).getIsCompleted()).isFalse();
        assertThat(result.getContent().get(1).getCompletionPercentage())
                .isEqualByComparingTo(BigDecimal.ZERO);
        verify(progressRepository).findByUserIdAndLessonIdIn(eq(99L), anyCollection());
    }

    @Test
    void levelFilterIsForwardedToRepository() {
        Pageable pageable = PageRequest.of(0, 12);
        when(lessonRepository.findPublishedPageProjection(null, LessonLevel.INTERMEDIATE, pageable))
                .thenReturn(Page.empty(pageable));

        Page<LessonListItemResponse> result =
                lessonService.getPublishedLessonPage(null, null, LessonLevel.INTERMEDIATE, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(lessonRepository).findPublishedPageProjection(null, LessonLevel.INTERMEDIATE, pageable);
    }

    @Test
    void deleteLesson_deletesAllChildTablesBeforeLesson() {
        Lesson lesson = lesson(1L, "Delete Me");

        when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));

        lessonService.deleteLesson(1L);

        // audit-v15: the Lesson Builder "Đường B" children (lesson_sections,
        // lesson_blocks, lesson_snapshots) no longer exist, so their cascade
        // verifications were removed with the feature.
        verify(lessonSubmissionRepository).deleteByLessonId(1L);
        verify(progressRepository).deleteByLessonId(1L);
        verify(vocabularyRepository).deleteByLessonId(1L);
        verify(speakingPromptRepository).deleteByLessonId(1L);
        verify(exerciseAttemptRepository).deleteAllByLessonId(1L);
        verify(exerciseRepository).deleteAllByLessonId(1L);
        verify(lessonRepository).delete(lesson);
    }
}
