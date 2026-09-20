package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.AdminUserDTO;
import com.datn.engflow.model.dto.response.LessonSummaryDTO;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.User;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Mock
    private StreakService streakService;

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

    /**
     * Danh sách user từng gọi {@code getCurrentStreak} cho từng row — mỗi call đọc
     * bảng {@code study_days} một lần, nên một trang 100 dòng tốn ~100 query. Streak
     * phải được lấy một lần cho cả trang.
     */
    @Test
    void usersReadStreaksOnceForWholePage() {
        User first = User.builder().id(1L).username("first").build();
        User second = User.builder().id(2L).username("second").build();
        // getAllUsers tự dựng pageable kèm Sort nên phải match bằng any(Pageable.class),
        // không dùng chính xác đối tượng pageable của test.
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 2));
        when(streakService.currentStreaks(anyCollection())).thenReturn(Map.of(1L, 5, 2L, 0));

        Page<AdminUserDTO> result = adminService.getAllUsers(null, 0, 2);

        assertThat(result.getContent()).extracting(AdminUserDTO::getCurrentStreak).containsExactly(5, 0);
        verify(streakService, times(1)).currentStreaks(anyCollection());
        verify(streakService, never()).getCurrentStreak(anyLong());
    }
}