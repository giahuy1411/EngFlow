package com.datn.engflow.model.dto.projection;

import java.time.LocalDate;

/**
 * Cặp {@code (userId, studyDate)} cho truy vấn batch của streak.
 *
 * <p>{@code StudyDay.userId} là một cột {@code Long} trần, không phải association,
 * nên không thể join sang {@code User} để lấy streak theo trang. Projection này cho
 * phép đọc mọi ngày học của cả một trang trong MỘT query thay vì một query mỗi row.</p>
 */
public interface StudyDayOwner {

    Long getUserId();

    LocalDate getStudyDate();
}
