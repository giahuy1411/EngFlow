package com.datn.engflow.model.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Một snapshot dùng chung ngày server cho cả chuỗi, trạng thái và lịch. */
public record StudySnapshot(LocalDate today, int currentStreak, boolean studiedToday,
                            LocalDate effectiveFrom, List<LocalDate> studiedDays,
                            List<LocalDate> legacyAccessDays, boolean legacyHistoryAvailable) {
}
