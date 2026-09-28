package com.datn.engflow.model.dto.response;

import lombok.*;

/**
 * Các con số đếm toàn hệ thống cho trang quản trị.
 *
 * <p>Tầng response của luồng {@code GET /api/admin/stats}: {@link
 * com.datn.engflow.controller.AdminController} nhận DTO này từ
 * {@code AdminService.getDashboardStats()}, mỗi trường đếm từ một repository khác
 * nhau (user, lesson, vocabulary, exercise, lesson-submission).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsDTO {
    private long totalUsers;
    private long totalLessons;
    private long totalVocabulary;
    private long activeUsers;
    private long recentUsers; // Users who studied in the last 7 days
    private long totalExercises;
    private long totalSubmissions;
}


