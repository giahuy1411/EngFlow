package com.datn.engflow.model.dto.response;

import lombok.*;

import java.util.List;

/**
 * Số liệu tổng quan của một người học cho trang dashboard.
 *
 * <p>Tầng response của {@code GET /api/dashboard/stats}: {@code DashboardController}
 * nhận DTO này từ {@code DashboardService.getDashboardStats()}. Ba trường cuối
 * ({@code totalExercises}, {@code correctExercises}, {@code dailyPoints}) là phần
 * hợp đồng cũ — nguồn dữ liệu đã bị gỡ nên service trả 0/empty, giữ lại để không
 * phá client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer totalPoints;
    private Integer currentStreak;
    private Integer totalVocabulary;
    private Integer totalExercises;
    private Integer correctExercises;
    private List<DailyPointEntry> dailyPoints;

    /**
     * Điểm kiếm được trong một ngày, dùng để vẽ biểu đồ.
     *
     * @param date ngày theo định dạng chuỗi, khớp với khóa ngày mà client dùng
     * @param points điểm trong ngày
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyPointEntry {
        private String date;
        private Integer points;
    }
}
