package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    
    private static final String LOGIN_DAYS_KEY_PREFIX = "user:login_days:";
    private static final long LOGIN_DAYS_TTL_DAYS = 90;

    @Transactional
    public void recordAccess(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDate today = LocalDate.now();
        
        LocalDate lastStudyDate = user.getLastStudyDate();
        
        boolean isFirstLoginToday = false;
        
        if (lastStudyDate == null) {
            user.setCurrentStreak(1);
            isFirstLoginToday = true;
        } else {
            long daysBetween = ChronoUnit.DAYS.between(lastStudyDate, today);
            
            if (daysBetween == 1) {
                user.setCurrentStreak(user.getCurrentStreak() + 1);
                isFirstLoginToday = true;
            } else if (daysBetween > 1) {
                user.setCurrentStreak(1);
                isFirstLoginToday = true;
            }
        }

        user.setLastStudyDate(today);
        userRepository.save(user);
        
        if (isFirstLoginToday) {
            recordLoginDateInRedis(userId, today);
        }
    }
    
    public void checkin(Long userId, Integer totalQuestions, int score) {
        recordAccess(userId);
    }

    private void recordLoginDateInRedis(Long userId, LocalDate date) {
        String key = LOGIN_DAYS_KEY_PREFIX + userId;
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        redisTemplate.opsForSet().add(key, dateStr);
        // Ensure TTL is set
        redisTemplate.expire(key, LOGIN_DAYS_TTL_DAYS, TimeUnit.DAYS);
    }

    public List<String> getLoginDays(Long userId, int days) {
        String key = LOGIN_DAYS_KEY_PREFIX + userId;
        Set<String> loginDaysSet = redisTemplate.opsForSet().members(key);
        
        if (loginDaysSet == null || loginDaysSet.isEmpty()) {
            return List.of();
        }
        
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        
        return loginDaysSet.stream()
                .filter(dateStr -> {
                    LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    return !date.isBefore(cutoffDate);
                })
                .sorted()
                .collect(Collectors.toList());
    }

    public Integer getCurrentStreak(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDate lastStudyDate = user.getLastStudyDate();
        
        if (lastStudyDate == null) {
            return 0;
        }
        
        LocalDate today = LocalDate.now();
        
        if (ChronoUnit.DAYS.between(lastStudyDate, today) > 1) {
            return 0;
        }
        return user.getCurrentStreak();
    }

    public List<User> getUsersWhoHaveNotLoggedInFor24Hours() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return userRepository.findUsersWhoHaveNotLoggedInSince(yesterday);
    }
}
