package com.datn.engflow.model.dto.projection;

import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.model.enums.SkillType;

/**
 * Projection gọn cho trang danh sách bài học.
 *
 * <p>Chỉ chọn đúng các cột mà UI danh sách thực sự render, nhờ đó tránh hydrate
 * hai cột lớn NVARCHAR(MAX) {@code content}/{@code contentOriginal} — chính hai
 * cột này chi phối số lần đọc trang (bảng lessons nặng ~161 MB cho vỏn vẹn
 * 1 472 dòng). Xem thêm {@link LessonTitle} và {@link ExerciseLessonProjection}
 * dùng cùng kỹ thuật.
 */
public interface LessonListProjection {

    /** @return id bài học. */
    Long getId();

    /** @return tiêu đề. */
    String getTitle();

    /** @return mô tả ngắn. */
    String getDescription();

    /** @return trình độ (badge). */
    LessonLevel getLevel();

    /** @return nhóm tài nguyên. */
    String getCategory();

    /** @return thời lượng ước tính (phút). */
    Integer getDurationMinutes();

    /** @return URL ảnh thumbnail. */
    String getThumbnailUrl();

    /** @return URL audio (nếu có). */
    String getAudioUrl();

    /** @return kỹ năng chính (quyết định component render). */
    SkillType getSkillType();

    /** @return thứ tự hiển thị. */
    Integer getOrderIndex();
}
