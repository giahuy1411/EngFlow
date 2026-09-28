package com.datn.engflow.model.dto.response;

import lombok.*;

/**
 * Tóm tắt tiến độ học gọn cho header và trang tiến độ.
 *
 * <p>Tầng response của {@code GET /api/users/progress}; {@code ProgressService}
 * đếm bài hoàn thành bằng cách lọc {@code progress} theo
 * {@code isCompleted} — khác {@code DashboardService} vốn đẩy xuống SQL.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressResponse {
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer totalPoints;
}
