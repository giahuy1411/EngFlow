package com.datn.engflow.model.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Bài nộp kỹ năng của một người học cho một bài học.
 *
 * <p>Tầng response của {@code /api/lesson-submissions}: dùng cho cả lúc nộp mới
 * và lúc đọc lại bài đã nộp. {@code LessonSubmissionService} gắn luôn
 * {@code username}/{@code fullName}/{@code lessonTitle} để client không phải gọi
 * thêm endpoint khác.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonSubmissionDTO {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private Long lessonId;
    private String lessonTitle;
    private String skillType;
    private String submissionText;
    private String audioUrl;
    private Double score;
    private String feedback;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
