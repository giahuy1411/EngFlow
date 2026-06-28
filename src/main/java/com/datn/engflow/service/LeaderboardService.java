package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.LeaderboardEntryDTO;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
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
public class LeaderboardService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeaderboard(int limit) {
        log.info("L\u1ea5y leaderboard v\u1edbi limit={}", limit);
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
                    .currentLevel(u.getCurrentLevel() != null ? u.getCurrentLevel().name() : "ELEMENTARY")
                    .build());
        }
        return leaderboard;
    }
}
