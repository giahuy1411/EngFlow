package com.datn.engflow.service.impl;

import com.datn.engflow.model.dto.response.LeaderboardEntryDTO;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeaderboard(int limit) {
        log.info("Lấy leaderboard với limit={}", limit);
        if (limit < 1) limit = 1;
        if (limit > 100) limit = 100;

        List<User> topUsers = userRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "totalPoints"))
        ).getContent();

        List<LeaderboardEntryDTO> leaderboard = new ArrayList<>();
        for (int i = 0; i < topUsers.size(); i++) {
            User u = topUsers.get(i);
            leaderboard.add(LeaderboardEntryDTO.builder()
                    .rank(i + 1)
                    .userId(u.getId())
                    .username(u.getUsername())
                    .fullName(u.getFullName())
                    .avatarUrl(u.getAvatarUrl())
                    .totalPoints(u.getTotalPoints())
                    .currentStreak(u.getCurrentStreak() != null ? u.getCurrentStreak() : 0)
                    .currentLevel(u.getCurrentLevel() != null ? u.getCurrentLevel().name() : "BEGINNER")
                    .build());
        }
        return leaderboard;
    }
}
