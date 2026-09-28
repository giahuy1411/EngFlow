package com.datn.engflow.model.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Một snapshot dùng chung ngày server cho cả chuỗi, trạng thái và lịch.
 *
 * <p>Tầng response của {@code GET /api/streak/snapshot}: {@code StreakController}
 * trả thẳng record này từ {@code StudyActivityService.snapshot}, nên client đóng
 * khung ngày theo {@code today} của server thay vì đồng hồ máy. Lịch bắt đầu từ
 * {@code effectiveFrom} (ngày mà {@code study_policy} mở lịch sử); phần ngày còn
 * sót trong Redis nằm trước mốc đó được tách riêng và có cờ báo Redis còn đọc
 * được hay không.
 */
public record StudySnapshot(LocalDate today, int currentStreak, boolean studiedToday,
                            LocalDate effectiveFrom, List<LocalDate> studiedDays,
                            List<LocalDate> legacyAccessDays, boolean legacyHistoryAvailable) {
}
