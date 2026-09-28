package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.response.ProgressResponse;
import com.datn.engflow.model.entity.Progress;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.ProgressRepository;
import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Tổng hợp tiến độ học của một người dùng: số bài đã publish, số bài đã hoàn thành và điểm tích lũy.
 *
 * <p>Tầng service, được gọi từ {@code ProgressController}. Đọc user, bài học và bản ghi tiến độ
 * lần lượt qua {@link UserRepository}, {@link LessonRepository}, {@link ProgressRepository}.
 */
public class ProgressService {

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;

    /**
     * Tổng kết tiến độ học cho người dùng đang đăng nhập.
     *
     * @param userEmail email người dùng
     * @return DTO gồm tổng bài đã publish, số bài hoàn thành và tổng điểm
     * @throws ResourceNotFoundException nếu không tìm thấy user theo email
     */
    @Transactional(readOnly = true)
    public ProgressResponse getProgressSummary(String userEmail) {
        log.info("L\u1ea5y th\u00f4ng tin t\u1ed5ng k\u1ebft ti\u1ebfn \u0111\u1ed9 cho user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        int totalLessons = (int) lessonRepository.countByIsPublishedTrue();

        List<Progress> progresses = progressRepository.findByUserId(user.getId());
        int completedLessons = (int) progresses.stream().filter(p -> Boolean.TRUE.equals(p.getIsCompleted())).count();

        return ProgressResponse.builder()
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .totalPoints(user.getTotalPoints())
                .build();
    }
}
