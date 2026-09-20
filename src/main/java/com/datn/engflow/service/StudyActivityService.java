package com.datn.engflow.service;

import com.datn.engflow.config.RedisConstants;
import com.datn.engflow.model.dto.projection.StudyDayOwner;
import com.datn.engflow.model.dto.response.StudySnapshot;
import com.datn.engflow.model.entity.StudyDay;
import com.datn.engflow.repository.StudyDayRepository;
import com.datn.engflow.repository.StudyPolicyRepository;
import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Ghi ngày học cùng transaction của kết quả đã được backend xác thực. */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyActivityService {
    private static final ZoneId STUDY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final UserRepository users;
    private final StudyDayRepository days;
    private final StudyPolicyRepository policies;
    private final StringRedisTemplate redis;
    private final Clock clock;

    /**
     * Khóa user trước khi kiểm tra unique day; caller phải có transaction lưu kết quả.
     *
     * <p><b>audit-v10 F122:</b> trước ngày cutover thì KHÔNG ghi ngày học, nhưng cũng
     * KHÔNG ném lỗi. Trước đây nhánh này ném {@code IllegalStateException}, và vì
     * {@code recordStudy} chạy trong chính transaction đang lưu kết quả của người gọi
     * (propagation MANDATORY), cú ném đó cuốn luôn kết quả học tập và thoát ra thành
     * HTTP 500 — nghĩa là toàn bộ khoảng thời gian trước cutover biến mọi endpoint
     * nộp bài tập, ôn SRS, điểm danh game và nộp speaking thành 500. "Hôm nay chưa tới
     * ngày hiệu lực" là một trạng thái cấu hình hợp lệ (streak đang ngủ), không phải
     * sự cố; còn thiếu hẳn row policy vẫn phải ném để lộ lỗi cấu hình (xem
     * {@link #effectiveFrom()}).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordStudy(Long userId) {
        LocalDate today = today();
        LocalDate start = effectiveFrom();
        if (today.isBefore(start)) {
            return;
        }
        var user = users.findForStudyUpdate(userId).orElseThrow();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("Inactive users cannot record study activity");
        }
        if (!days.existsByUserIdAndStudyDate(userId, today)) {
            days.save(new StudyDay(userId, today));
        }
    }

    @Transactional(readOnly = true)
    public StudySnapshot snapshot(Long userId, int window) {
        if (window < 1 || window > 366) {
            throw new IllegalArgumentException("Study history window must be between 1 and 366 days");
        }
        LocalDate today = today();
        LocalDate start = effectiveFrom();
        LocalDate cutoff = today.minusDays(window - 1L);
        List<LocalDate> allDays = days.findDates(userId, start, today);
        List<LocalDate> visibleDays = allDays.stream().filter(date -> !date.isBefore(cutoff))
                .sorted().toList();
        List<LocalDate> legacyDays = new ArrayList<>();
        boolean legacyAvailable = true;
        try {
            var members = redis.opsForSet().members(RedisConstants.LOGIN_DAYS_KEY_PREFIX + userId);
            if (members != null) {
                for (String member : members) {
                    try {
                        LocalDate date = LocalDate.parse(member);
                        if (!date.isBefore(cutoff) && date.isBefore(start) && !date.isAfter(today)) {
                            legacyDays.add(date);
                        }
                    } catch (DateTimeParseException invalidLegacyDate) {
                        legacyAvailable = false;
                    }
                }
            }
        } catch (RuntimeException unavailable) {
            legacyAvailable = false;
        }
        legacyDays.sort(Comparator.naturalOrder());
        return new StudySnapshot(today, currentStreak(allDays, today), allDays.contains(today),
                start, visibleDays, List.copyOf(legacyDays), legacyAvailable);
    }

    @Transactional(readOnly = true)
    public int currentStreak(Long userId) {
        LocalDate today = today();
        return currentStreak(days.findDates(userId, effectiveFrom(), today), today);
    }

    /**
     * Streak của nhiều user trong MỘT query, dùng cho các trang danh sách.
     *
     * <p>Gọi {@link #currentStreak(Long)} theo từng row là N+1: một trang 20 dòng của
     * leaderboard tốn 21 query. Ở đây đọc mọi ngày học của cả trang một lần rồi tính
     * trong bộ nhớ — và vẫn đi qua đúng hàm {@link #currentStreak(List, LocalDate)}
     * để hai đường không bao giờ lệch luật.</p>
     *
     * @return userId → streak hiệu lực; user không có ngày học nào nhận 0.
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> currentStreaks(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        LocalDate today = today();
        LocalDate start = effectiveFrom();
        Map<Long, List<LocalDate>> byUser = days.findDatesForUsers(userIds, start, today).stream()
                .collect(Collectors.groupingBy(StudyDayOwner::getUserId,
                        Collectors.mapping(StudyDayOwner::getStudyDate, Collectors.toList())));
        Map<Long, Integer> streaks = new HashMap<>();
        for (Long userId : userIds) {
            streaks.put(userId, currentStreak(byUser.getOrDefault(userId, List.of()), today));
        }
        return streaks;
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(STUDY_ZONE));
    }

    @Transactional(readOnly = true)
    public List<com.datn.engflow.model.entity.User> reminderCandidates(boolean atRisk) {
        LocalDate today = today();
        LocalDate start = effectiveFrom();
        return atRisk ? users.findStudyReminderAtRisk(start, today, today.minusDays(1))
                : users.findStudyReminderBroken(start, today, today.minusDays(1));
    }

    @Transactional(readOnly = true)
    public boolean reminderEligible(Long userId, boolean atRisk) {
        var user = users.findById(userId).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) return false;
        LocalDate today = today();
        var dates = days.findDates(userId, effectiveFrom(), today);
        if (dates.isEmpty()) return false;
        LocalDate last = dates.stream().max(Comparator.naturalOrder()).orElseThrow();
        return atRisk ? last.equals(today.minusDays(1)) : last.isBefore(today.minusDays(1));
    }

    @Transactional(readOnly = true)
    public int lastCompletedStreak(Long userId) {
        var dates = days.findDates(userId, effectiveFrom(), today());
        return dates.isEmpty() ? 0 : currentStreak(dates, dates.stream().max(Comparator.naturalOrder()).orElseThrow());
    }

    /**
     * Mốc cutover đã chốt. Thiếu row là lỗi triển khai, không phải trạng thái hợp lệ:
     * Hibernate {@code ddl-auto=update} tạo được bảng nhưng cố ý KHÔNG tạo row policy,
     * vì ngày hiệu lực phải là một quyết định được review chứ không phải giá trị mặc
     * định lúc boot. Vì vậy ở đây ném thay vì đoán — nhưng thông báo phải nói rõ file
     * cần chạy, nếu không người trực ca chỉ thấy một IllegalStateException trần.
     */
    private LocalDate effectiveFrom() {
        return policies.findById(1).orElseThrow(() -> {
            log.error("study_policy row (id=1) is missing — streak cannot compute an effective date. "
                    + "Run the reviewed deployment script tasks/streak-study/deploy.sql "
                    + "(-v EffectiveFrom=YYYY-MM-DD BackupFile=<server-local backup path>) "
                    + "before starting this backend.");
            return new IllegalStateException(
                    "Study policy is missing; run tasks/streak-study/deploy.sql before starting the backend");
        }).getEffectiveFrom();
    }

    private int currentStreak(List<LocalDate> dates, LocalDate today) {
        var uniqueDates = new java.util.HashSet<>(dates);
        LocalDate cursor = uniqueDates.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (uniqueDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
