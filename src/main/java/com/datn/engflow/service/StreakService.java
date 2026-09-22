package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Facade tương thích cho các chỗ còn đọc streak. Mọi giá trị đều lấy từ
 * {@link StudyActivityService} (nguồn sự thật là bảng SQL {@code study_days});
 * không còn nơi nào tự đếm ngày đăng nhập.
 */
@Service
@RequiredArgsConstructor
public class StreakService {
    private final StudyActivityService studyActivityService;

    @Transactional
    public void checkin(Long userId, Integer totalQuestions, int score) {
        if (totalQuestions != null && totalQuestions > 0) {
            studyActivityService.recordStudy(userId);
        }
    }

    public Integer getCurrentStreak(Long userId) {
        return studyActivityService.currentStreak(userId);
    }

    /** Streak cho cả một trang danh sách trong một query — xem {@link StudyActivityService#currentStreaks}. */
    public java.util.Map<Long, Integer> currentStreaks(java.util.Collection<Long> userIds) {
        return studyActivityService.currentStreaks(userIds);
    }

    public String todayIso() {
        return studyActivityService.today().toString();
    }

    public List<String> getLoginDays(Long userId, int days) {
        return studyActivityService.snapshot(userId, days).studiedDays().stream()
                .map(java.time.LocalDate::toString).toList();
    }

    public List<User> getUsersWithStreakAtRisk() {
        return studyActivityService.reminderCandidates(true);
    }

    public List<User> getUsersWithBrokenStreak() {
        return studyActivityService.reminderCandidates(false);
    }

    public boolean reminderEligible(Long userId, boolean atRisk) {
        return studyActivityService.reminderEligible(userId, atRisk);
    }

    public int lastCompletedStreak(Long userId) {
        return studyActivityService.lastCompletedStreak(userId);
    }

    /**
     * audit-v13 F-13-08: distinct learners active in the last N days, from the real
     * {@code study_days} calendar (not the stale {@code users.last_study_date} column).
     */
    public long countActiveLearnersInLastDays(int days) {
        return studyActivityService.countActiveLearnersInLastDays(days);
    }
}
