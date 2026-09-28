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

/**
 * Ghi ngày học cùng transaction của kết quả đã được backend xác thực.
 *
 * <p>Tầng service, là nguồn sự thật DUY NHẤT cho streak: mọi ngày học nằm trong
 * bảng {@code study_days} ({@link StudyDayRepository}) và được đọc lại qua đây.
 * {@link StreakService} chỉ là facade mỏng; không có đường nào khác tự đếm ngày.
 *
 * <p>Hai quy ước ràng buộc mọi caller: (1) {@link #recordStudy} khai báo
 * {@link Propagation#MANDATORY} nên phải được gọi từ trong một transaction đang
 * lưu kết quả học tập — nhờ đó ngày học không bao giờ tồn tại cho một lượt làm bài
 * đã rollback; (2) mọi phép so sánh ngày đều đi qua {@link #today()} (múi giờ
 * {@code Asia/Ho_Chi_Minh}, đọc từ {@link Clock} bean) chứ không dùng
 * {@code LocalDate.now()} trần, để test cuốn được thời gian.
 *
 * <p>Ngưỡng tính streak không nằm trong class mà đọc từ row {@code study_policy}
 * ({@link StudyPolicyRepository}) — xem {@link #effectiveFrom()}.
 */
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
     *
     * @param userId user vừa hoàn thành một hoạt động học tập đã xác thực
     * @throws java.util.NoSuchElementException nếu không tìm thấy user theo id
     * @throws IllegalStateException nếu user đã bị vô hiệu hóa, hoặc thiếu row
     *         {@code study_policy} (mốc cutover chưa được deploy)
     * @throws org.springframework.transaction.IllegalTransactionStateException
     *         nếu được gọi ngoài transaction (propagation MANDATORY)
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

    /**
     * Lịch học của user trong một cửa sổ N ngày, kèm streak và cờ hôm nay đã học.
     *
     * <p>Trả về cả ngày ghi trong bảng {@code study_days} lẫn ngày legacy còn nằm
     * trong Redis (key {@code user:login_days:&lt;id&gt;}) để lịch không bị trống trước
     * ngày cutover. Ngày legacy đọc lỗi hoặc sai định dạng chỉ làm hỏng phần
     * legacy, không làm hỏng lịch chính — cờ {@code legacyAvailable} báo cho UI biết
     * có nên render phần đó không.
     *
     * @param userId user cần tra
     * @param window độ rộng cửa sổ tính ngược từ hôm nay, 1..366
     * @return snapshot gồm hôm nay, streak, lịch ngày và ngày cutover
     * @throws IllegalArgumentException nếu {@code window} nằm ngoài 1..366
     * @throws IllegalStateException nếu thiếu row {@code study_policy}
     */
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

    /**
     * Chuỗi ngày học liên tiếp đang hiệu lực của một user.
     *
     * <p>Chuỗi được tính từ hôm nay lùi về quá khứ; nếu hôm nay chưa học thì xuất
     * phát từ hôm qua, nên chuỗi vẫn sống cho tới hết ngày hôm.
     *
     * @param userId user cần tra
     * @return độ dài chuỗi; 0 nếu user chưa có ngày học nào
     * @throws IllegalStateException nếu thiếu row {@code study_policy}
     */
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
     * @param userIds các user cần tra; null hoặc rỗng trả về map rỗng
     * @return userId → streak hiệu lực; user không có ngày học nào nhận 0.
     * @throws IllegalStateException nếu thiếu row {@code study_policy}
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

    /**
     * Ngày học tập "hôm nay" theo múi giờ Việt Nam.
     *
     * <p>Mọi so sánh ngày trong class này dùng hàm này thay vì
     * {@code LocalDate.now()} trần: container có thể chạy ở UTC, lệch 7 giờ so với
     * giờ VN, và streak sẽ đóng/mở sai ngày.
     *
     * @return ngày hiện tại theo {@code Asia/Ho_Chi_Minh}
     */
    public LocalDate today() {
        return LocalDate.now(clock.withZone(STUDY_ZONE));
    }

    /**
     * audit-v13 F-13-08: how many distinct users studied in the last {@code windowDays} days.
     * The admin dashboard used to answer this from the legacy {@code users.last_study_date}
     * column, which is no longer maintained by the streak refactor (study_days is the
     * source of truth). This reads the real calendar instead.
     *
     * <p>Window semantics match {@link #snapshot(Long, int)}: "last 7 days" means today and
     * the six days before it (inclusive), i.e. {@code today - (windowDays - 1)} .. today.
     *
     * <p>Khác {@link #snapshot(Long, int)}, hàm này KHÔNG lọc theo mốc cutover: nó
     * đếm trực tiếp trên {@code study_days} nên trước ngày hiệu lực vẫn trả về số
     * thật, và không phụ thuộc row {@code study_policy}.
     *
     * @param windowDays độ rộng cửa sổ, tính cả hôm nay
     * @return số user phân biệt được có ít nhất một ngày học trong cửa sổ
     */
    @Transactional(readOnly = true)
    public long countActiveLearnersInLastDays(int windowDays) {
        LocalDate end = today();
        return days.countDistinctUsersBetween(end.minusDays(windowDays - 1L), end);
    }

    /**
     * Danh sách user ứng viên nhận mail nhắc, theo nhánh at-risk hay broken.
     *
     * <p>Cả hai nhánh đều yêu cầu user ĐÃ TỪNG học (có ít nhất một ngày trong
     * {@code study_days}) và đã học lần cuối vào đúng hôm qua; khác nhau ở chỗ nhánh
     * at-risk chấp nhận "học hôm qua, chưa học hôm nay" còn nhánh broken yêu cầu
     * ngày học cuối NỘT hơn hôm qua. Người chưa từng học không thuộc tập nào.
     *
     * @param atRisk {@code true} lấy nhánh at-risk, {@code false} lấy nhánh broken
     * @return danh sách user ứng viên
     * @throws IllegalStateException nếu thiếu row {@code study_policy}
     */
    @Transactional(readOnly = true)
    public List<com.datn.engflow.model.entity.User> reminderCandidates(boolean atRisk) {
        LocalDate today = today();
        LocalDate start = effectiveFrom();
        return atRisk ? users.findStudyReminderAtRisk(start, today, today.minusDays(1))
                : users.findStudyReminderBroken(start, today, today.minusDays(1));
    }

    /**
     * Kiểm tra lại điều kiện nhắc cho MỘT user, dùng để chặn cuối ngay trước lúc gửi.
     *
     * <p>Tách riêng khỏi {@link #reminderCandidates} vì giữa lúc quét danh sách và
     * lúc gửi mail user có thể đã học — nếu gửi cả mail at-risk lẫn comeback cho
     * cùng một người thì cả hai vẫn "đúng" theo danh sách cũ.
     *
     * @param userId user cần tra
     * @param atRisk {@code true} cho nhánh at-risk, {@code false} cho nhánh broken
     * @return {@code true} nếu user còn đúng trạng thái cần nhắc
     * @throws IllegalStateException nếu thiếu row {@code study_policy}
     */
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

    /**
     * Độ dài chuỗi đã đóng tại ngày học cuối cùng — số liệu đưa vào mail mời quay lại.
     *
     * <p>Tính lại từ ngày học CUỐI, không phải từ hôm nay: nếu tính từ hôm nay thì
     * chuỗi của một người đã bỏ học 5 ngày luôn bằng 0 và mail mời quay lại mất
     * hết thông tin.
     *
     * @param userId user cần tra
     * @return độ dài chuỗi tính tới ngày học cuối; 0 nếu chưa từng học
     * @throws IllegalStateException nếu thiếu row {@code study_policy}
     */
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
     *
     * @return ngày hiệu lực đã duyệt, mọi ngày học trước ngày này bị bỏ qua
     * @throws IllegalStateException nếu bảng {@code study_policy} không có row id=1
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

    /**
     * Thuật toán tính streak dùng chung cho cả một user lẫn nhiều user.
     *
     * <p>Chấp nhận {@code today} làm tham số thay vì tự gọi {@link #today()} để hai
     * đường đọc (đơn lẻ và theo trang) không thể lệch nhau và để test truyền được
     * mốc thời gian. Ngày trùng lặp bị khử bằng {@code HashSet} trước khi đếm.
     *
     * @param dates các ngày đã học
     * @param today mốc "hôm nay" để tính ngược
     * @return độ dài chuỗi tính từ {@code today} (hoặc từ hôm qua nếu hôm nay
     *         chưa học) lùi về quá khứ
     */
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
