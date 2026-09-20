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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServicePaginationTest {

    @Mock
    private UserRepository userRepository;

    /**
     * {@code LeaderboardService.toEntry} reads the streak through {@code StreakService},
     * so the collaborator must exist for {@code @InjectMocks} to construct the service.
     * Lenient because rank/pagination assertions never inspect the streak value.
     */
    @Mock
    private StreakService streakService;

    @InjectMocks
    private LeaderboardService leaderboardService;

    @Test
    void assignsGlobalRanksAcrossPages() {
        User third = User.builder().id(30L).username("third").totalPoints(70).build();
        User fourth = User.builder().id(40L).username("fourth").totalPoints(60).build();
        Pageable pageable = PageRequest.of(1, 2);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(third, fourth), pageable, 4));

        Page<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(1, 2);

        assertThat(result.getContent()).extracting(LeaderboardEntryDTO::getRank).containsExactly(3, 4);
        assertThat(result.getTotalElements()).isEqualTo(4);
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    void clampsPageSizeToOneHundred() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        leaderboardService.getLeaderboard(0, 500);

        verify(userRepository).findAll(PageRequest.of(0, 100, LeaderboardService.LEADERBOARD_SORT));
    }
}
