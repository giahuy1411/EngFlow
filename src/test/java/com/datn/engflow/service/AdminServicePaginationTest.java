package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.LessonSummaryDTO;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.LessonSubmissionRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServicePaginationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private LessonSubmissionRepository lessonSubmissionRepository;

    @Mock
    private LessonService lessonService;

    @InjectMocks
    private AdminService adminService;

    @Test
    void lessonsAreReturnedAsPaginatedSummaries() {
        Pageable pageable = PageRequest.of(0, 20);
        Lesson lesson = Lesson.builder()
                .id(1L)
                .title("Intro to English")
                .description("Basic lesson")
                .build();
        Page<Lesson> expected = new PageImpl<>(List.of(lesson), pageable, 1);
        when(lessonRepository.findAdminPage(null, null, pageable)).thenReturn(expected);

        Page<LessonSummaryDTO> result = adminService.getAllLessonsAdmin(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Intro to English");
        verify(lessonRepository).findAdminPage(null, null, pageable);
    }

    @Test
    void lessonsCanBeFilteredByKeywordWithPaging() {
        Pageable pageable = PageRequest.of(1, 10);
        Lesson lesson = Lesson.builder()
                .id(2L)
                .title("Business English")
                .description("Vocabulary for work")
                .build();
        Page<Lesson> expected = new PageImpl<>(List.of(lesson), pageable, 1);
        when(lessonRepository.findAdminPage("business", null, pageable)).thenReturn(expected);

        Page<LessonSummaryDTO> result = adminService.getAllLessonsAdmin("  business  ", pageable);

        assertThat(result.getContent()).extracting(LessonSummaryDTO::getTitle)
                .containsExactly("Business English");
        verify(lessonRepository).findAdminPage("business", null, pageable);
    }
}