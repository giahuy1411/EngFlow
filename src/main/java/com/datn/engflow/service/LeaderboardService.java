package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.LeaderboardEntryDTO;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Xếp hạng người học theo điểm tích lũy.
 *
 * <p>Tầng service, được gọi từ {@code LeaderboardController}. Đọc user qua
 * {@link UserRepository} theo thứ tự điểm giảm dần rồi gắn streak hiệu lực lấy từ
 * {@link StreakService} (một query cho cả trang, tránh N+1).
 */
public class LeaderboardService {

    public static final Sort LEADERBOARD_SORT = Sort.by(
            Sort.Order.desc("totalPoints"),
            Sort.Order.asc("id")
    );
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final StreakService streakService;

    /**
     * Trang xếp hạng, sắp theo điểm giảm dần rồi id tăng dần để thứ tự ổn định khi bằng điểm.
     *
     * <p>Hạng được tính từ vị trí tuyệt đối ({@code rankOffset + index + 1}) nên đúng cả khi
     * yêu cầu trang giữa. Streak của cả trang đọc trong một query; trang rỗng bỏ qua tầng streak.
     *
     * @param page chỉ số trang, 0-based
     * @param size số dòng mỗi trang, bị chặn trong khoảng 1..100
     * @return trang các mục xếp hạng
     */
    @Transactional(readOnly = true)
    public Page<LeaderboardEntryDTO> getLeaderboard(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, LEADERBOARD_SORT);
        Page<User> users = userRepository.findAll(pageable);
        long rankOffset = (long) safePage * safeSize;

        // Streak cho cả trang trong MỘT query: đọc từng row là N+1 (đo được +21 query
        // cho size=20). Trang rỗng thì không chạm tầng streak.
        List<Long> userIds = users.getContent().stream().map(User::getId).toList();
        Map<Long, Integer> streaks = userIds.isEmpty() ? Map.of() : streakService.currentStreaks(userIds);

        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        for (int index = 0; index < users.getContent().size(); index++) {
            User user = users.getContent().get(index);
            entries.add(toEntry(user, rankOffset + index + 1, streaks.getOrDefault(user.getId(), 0)));
        }

        return new PageImpl<>(entries, pageable, users.getTotalElements());
    }

    /**
     * Lấy N người đứng đầu bảng xếp hạng.
     *
     * @param limit số mục cần lấy, bị chặn trong khoảng 1..100
     * @return danh sách mục xếp hạng từ hạng 1
     */
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeaderboard(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
        return getLeaderboard(0, safeLimit).getContent();
    }

    /** Map entity User + hạng + streak sang DTO; level null mặc định là ELEMENTARY. */
    private LeaderboardEntryDTO toEntry(User user, long rank, int currentStreak) {
        return LeaderboardEntryDTO.builder()
                .rank(Math.toIntExact(rank))
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .totalPoints(user.getTotalPoints())
                .currentStreak(currentStreak)
                .currentLevel(user.getCurrentLevel() != null ? user.getCurrentLevel().name() : "ELEMENTARY")
                .build();
    }
}
