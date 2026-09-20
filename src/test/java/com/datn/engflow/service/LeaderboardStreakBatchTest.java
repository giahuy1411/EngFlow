package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.LeaderboardEntryDTO;
import com.datn.engflow.model.entity.User;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Leaderboard từng gọi {@code getCurrentStreak} cho TỪNG row, và mỗi call lại đọc
 * bảng {@code study_days} một lần: đo được +21 query cho một trang 20 dòng. Streak
 * phải được lấy một lần cho cả trang.
 */
@ExtendWith(MockitoExtension.class)
class LeaderboardStreakBatchTest {

    @Mock private UserRepository userRepository;
    @Mock private StreakService streakService;

    @InjectMocks private LeaderboardService leaderboardService;

    private User user(long id, String username) {
        return User.builder().id(id).username(username).totalPoints(10).build();
    }

    @Test
    void readsStreaksOnceForWholePage() {
        User first = user(1L, "first");
        User second = user(2L, "second");
        Pageable pageable = PageRequest.of(0, 2);
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));
        when(streakService.currentStreaks(anyCollection())).thenReturn(Map.of(1L, 7, 2L, 3));

        Page<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(0, 2);

        assertThat(result.getContent()).extracting(LeaderboardEntryDTO::getCurrentStreak)
                .containsExactly(7, 3);
        verify(streakService, times(1)).currentStreaks(anyCollection());
        verify(streakService, never()).getCurrentStreak(anyLong());
    }

    /** User không có ngày học nào phải hiện 0, không được NPE. */
    @Test
    void userMissingFromBatchShowsZero() {
        Pageable pageable = PageRequest.of(0, 1);
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user(9L, "ninth")), pageable, 1));
        when(streakService.currentStreaks(anyCollection())).thenReturn(Map.of());

        Page<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(0, 1);

        assertThat(result.getContent()).extracting(LeaderboardEntryDTO::getCurrentStreak).containsExactly(0);
    }

    /** Trang rỗng không được gọi xuống tầng streak. */
    @Test
    void emptyPageDoesNotQueryStreaks() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        leaderboardService.getLeaderboard(0, 20);

        verifyNoInteractions(streakService);
    }
}
