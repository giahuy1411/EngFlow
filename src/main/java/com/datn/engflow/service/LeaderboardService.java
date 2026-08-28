package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.LeaderboardEntryDTO;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * class LeaderboardService.
 */
public class LeaderboardService {

    public static final Sort LEADERBOARD_SORT = Sort.by(
            Sort.Order.desc("totalPoints"),
            Sort.Order.asc("id")
    );
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<LeaderboardEntryDTO> getLeaderboard(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, LEADERBOARD_SORT);
        Page<User> users = userRepository.findAll(pageable);
        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        long rankOffset = (long) safePage * safeSize;

        for (int index = 0; index < users.getContent().size(); index++) {
            User user = users.getContent().get(index);
            entries.add(toEntry(user, rankOffset + index + 1));
        }

        return new PageImpl<>(entries, pageable, users.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeaderboard(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
        return getLeaderboard(0, safeLimit).getContent();
    }

    private LeaderboardEntryDTO toEntry(User user, long rank) {
        return LeaderboardEntryDTO.builder()
                .rank(Math.toIntExact(rank))
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .totalPoints(user.getTotalPoints())
                .currentStreak(user.getCurrentStreak() != null ? user.getCurrentStreak() : 0)
                .currentLevel(user.getCurrentLevel() != null ? user.getCurrentLevel().name() : "ELEMENTARY")
                .build();
    }
}
