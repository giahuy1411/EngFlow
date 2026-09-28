package com.datn.engflow.model.dto.request;

import com.datn.engflow.model.entity.SpeakingPromptMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO tạo và sửa bài luyện nói, dùng chung cho {@code POST} và {@code PUT} trên cả hai cặp
 * đường dẫn {@code /api/v1/admin/speaking-prompts} và {@code /api/v1/admin/video-prompts}.
 *
 * <p>{@code SpeakingPromptService} áp ba quy tắc lên payload mà validation không thể biểu đạt:
 * {@code mode} null thì mặc định {@code FREE_SPEAKING}; mode {@code READ_ALOUD} bắt buộc có
 * {@link #referenceText}; và các trường null khi sửa được hiểu là "giữ nguyên" chứ không phải
 * "xoá" — riêng {@code lessonId} phải truyền lại khi muốn gỡ liên kết bài học.
 */
@Data
public class CreateSpeakingPromptRequest {
    @NotBlank
    private String title;
    private String description;
    @NotBlank
    private String prompt;
    private String level;
    private String category;
    private Boolean isPremium;
    private String thumbnailUrl;
    private Integer orderIndex;
    private Boolean isPublished;
    private Long lessonId;
    private SpeakingPromptMode mode;
    private String referenceText;
    private String referenceMediaObjectKey;
    private String referenceMediaUrl;
    @Min(15)
    @Max(600)
    private Integer maxDurationSeconds;
    @Min(1)
    @Max(100)
    private Integer attemptLimit;
}
