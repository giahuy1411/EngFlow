package com.datn.engflow.service;

import com.datn.engflow.model.entity.UserStreak;

import java.util.List;

public interface StreakService {
    UserStreak checkin(Long userId, int wordsStudied, int gamesPlayed, int coinsEarned);
    List<UserStreak> getStreakHistory(Long userId, int days);
    Integer getCurrentStreak(Long userId);
}
