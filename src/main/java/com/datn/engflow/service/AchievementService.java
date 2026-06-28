package com.datn.engflow.service;

import com.datn.engflow.model.entity.Achievement;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.AchievementRepository;
import com.datn.engflow.repository.UserAchievementRepository;
import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserRepository userRepository;
    private final UserAchievementRepository userAchievementRepository;

    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    public List<Long> getUnlockedAchievementIds(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .map(user -> userAchievementRepository.findAchievementIdsByUserId(user.getId()))
                .orElse(Collections.emptyList());
    }
}
