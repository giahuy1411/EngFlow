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
 *
 * <p>Tầng service, gọi bởi {@code StreakController} và bởi
 * {@link StreakReminderScheduler} cho job nhắc học 20:00. Class này KHÔNG giữ
 * logic nào: mỗi method chỉ đổi kiểu/thứ tự tham số rồi chuyển thẳng sang
 * {@link StudyActivityService}, nên có thể đổi cả hai cùng lúc mà không hỏng
 * luật tính streak. Cặp {@link #getCurrentStreak(Long)} / {@link #currentStreaks}
 * là hai đường đọc cùng một dữ liệu — dùng bản {@code currentStreaks} khi
 * render trang danh sách để không tạo N+1.
 */
@Service
@RequiredArgsConstructor
public class StreakService {
    private final StudyActivityService studyActivityService;

    /**
     * Ghi nhận ngày học hôm nay nếu lượt làm bài thực sự có câu hỏi.
     *
     * <p>Điều kiện đủ để tính là có câu hỏi ({@code totalQuestions > 0}) — chấm điểm
     * cao hay thấp không ảnh hưởng tới việc ghi ngày, nên {@code score} không dùng.
     *
     * @param userId user được ghi nhận
     * @param totalQuestions số câu trong lượt làm bài; null hoặc {@code <= 0} thì bỏ qua
     * @param score điểm của lượt làm bài, không dùng để quyết định ghi ngày
     * @throws java.util.NoSuchElementException nếu không tìm thấy user theo id
     * @throws IllegalStateException nếu user bị vô hiệu hóa, hoặc thiếu row
     *         {@code study_policy} — xem {@link StudyActivityService#effectiveFrom}
     */
    @Transactional
    public void checkin(Long userId, Integer totalQuestions, int score) {
        if (totalQuestions != null && totalQuestions > 0) {
            studyActivityService.recordStudy(userId);
        }
    }

    /**
     * Chuỗi ngày học liên tiếp đang hiệu lực của một user.
     *
     * @param userId user cần tra
     * @return độ dài chuỗi; 0 nếu user chưa có ngày học nào
     */
    public Integer getCurrentStreak(Long userId) {
        return studyActivityService.currentStreak(userId);
    }

    /**
     * Streak cho cả một trang danh sách trong một query — xem {@link StudyActivityService#currentStreaks}.
     *
     * @param userIds các user cần tra; null hoặc rỗng trả về map rỗng
     * @return map userId → streak, user không có ngày học nào nhận 0
     */
    public java.util.Map<Long, Integer> currentStreaks(java.util.Collection<Long> userIds) {
        return studyActivityService.currentStreaks(userIds);
    }

    /**
     * Ngày học "hôm nay" theo múi giờ học tập, định dạng ISO.
     *
     * @return chuỗi {@code yyyy-MM-dd}
     */
    public String todayIso() {
        return studyActivityService.today().toString();
    }

    /**
     * Danh sách ngày đã học trong cửa sổ N ngày gần nhất, dạng chuỗi ISO.
     *
     * @param userId user cần tra
     * @param days độ rộng cửa sổ tính ngược từ hôm nay
     * @return các ngày có học, tăng dần
     */
    public List<String> getLoginDays(Long userId, int days) {
        return studyActivityService.snapshot(userId, days).studiedDays().stream()
                .map(java.time.LocalDate::toString).toList();
    }

    /**
     * User có chuỗi đang "nguy cơ" — đã học hôm qua, chưa học hôm nay.
     *
     * @return danh sách user ứng viên nhận mail nhắc giữ chuỗi
     */
    public List<User> getUsersWithStreakAtRisk() {
        return studyActivityService.reminderCandidates(true);
    }

    /**
     * User đã bỏ học từ 2 ngày trở lên nhưng từng có ngày học.
     *
     * @return danh sách user ứng viên nhận mail mời quay lại
     */
    public List<User> getUsersWithBrokenStreak() {
        return studyActivityService.reminderCandidates(false);
    }

    /**
     * Kiểm tra lại điều kiện nhắc cho MỘT user ngay trước lúc gửi mail.
     *
     * @param userId user cần tra
     * @param atRisk {@code true} cho nhánh at-risk, {@code false} cho nhánh broken
     * @return {@code true} nếu user vẫn còn đúng trạng thái cần nhắc
     */
    public boolean reminderEligible(Long userId, boolean atRisk) {
        return studyActivityService.reminderEligible(userId, atRisk);
    }

    /**
     * Độ dài chuỗi đã hoàn thành gần nhất, dùng làm số liệu trong mail mời quay lại.
     *
     * @param userId user cần tra
     * @return độ dài chuỗi tính tới ngày học cuối cùng; 0 nếu chưa từng học
     */
    public int lastCompletedStreak(Long userId) {
        return studyActivityService.lastCompletedStreak(userId);
    }

    /**
     * audit-v13 F-13-08: distinct learners active in the last N days, from the real
     * {@code study_days} calendar (not the stale {@code users.last_study_date} column).
     *
     * @param days độ rộng cửa sổ, tính cả hôm nay
     * @return số user khác nhau có ít nhất một ngày học trong cửa sổ
     */
    public long countActiveLearnersInLastDays(int days) {
        return studyActivityService.countActiveLearnersInLastDays(days);
    }
}
