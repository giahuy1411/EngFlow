package com.datn.engflow.model.dto.request;

import com.datn.engflow.model.enums.LessonLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO tạo và sửa bài học, dùng chung cho phía quản trị ({@code POST/PUT
 * /api/admin/lessons}) và phía người dùng đã đăng nhập
 * ({@code LessonController.createLesson} / {@code updateLesson}); cả hai đều nhận
 * {@code @Valid @RequestBody}.
 *
 * <p>{@code level} là enum {@code LessonLevel} chứ không phải chuỗi, nên JSON phải gửi đúng
 * tên hằng; cùng quy ước đó, {@code skillType} ở đây lại là chuỗi tự do chứ không phải
 * {@code SkillType}.
 */
@Getter
@Setter
public class LessonRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    
    private String content;

    @NotNull(message = "Level is required")
    private LessonLevel level;

    private String category;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private String thumbnailUrl;

    private String audioUrl;

    private Integer orderIndex;

    private String skillType;
    
    private Boolean isPublished;
}
