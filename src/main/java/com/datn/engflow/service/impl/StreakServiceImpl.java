package com.datn.engflow.service.impl;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.UserStreak;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.UserStreakRepository;
import com.datn.engflow.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StreakServiceImpl implements StreakService {

    private final UserStreakRepository streakRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserStreak checkin(Long userId, int wordsStudied, int gamesPlayed, int coinsEarned) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDate today = LocalDate.now();
        
        UserStreak todayStreak = streakRepository.findByUserIdAndStudyDate(userId, today)
                .orElseGet(() -> {
                    // Update user's streak logic
                    LocalDate lastStudy = user.getLastStudyDate();
                    if (lastStudy == null || lastStudy.isBefore(today.minusDays(1))) {
                        user.setCurrentStreak(1);
                    } else if (lastStudy.equals(today.minusDays(1))) {
                        user.setCurrentStreak(user.getCurrentStreak() + 1);
                    }
                    user.setLastStudyDate(today);
                    userRepository.save(user);
                    
                    return UserStreak.builder()
                            .user(user)
                            .studyDate(today)
                            .wordsStudied(0)
                            .gamesPlayed(0)
                            .coinsEarned(0)
                            .build();
                });
                
        todayStreak.setWordsStudied(todayStreak.getWordsStudied() + wordsStudied);
        todayStreak.setGamesPlayed(todayStreak.getGamesPlayed() + gamesPlayed);
        todayStreak.setCoinsEarned(todayStreak.getCoinsEarned() + coinsEarned);
        
        return streakRepository.save(todayStreak);
    }

    @Override
    public List<UserStreak> getStreakHistory(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        return streakRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(userId, startDate, endDate);
    }

    @Override
    public Integer getCurrentStreak(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDate lastStudy = user.getLastStudyDate();
        
        if (lastStudy == null || lastStudy.isBefore(today.minusDays(1))) {
            return 0; // Lost streak
        }
        return user.getCurrentStreak();
    }
}
