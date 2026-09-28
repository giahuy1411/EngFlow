package com.datn.engflow.model.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bản đầy đủ của một bài học, dùng khi mở chi tiết hoặc khi admin ghi.
 *
 * <p>Tầng response của {@code GET /api/lessons/{id}}, {@code POST} và {@code PUT}
 * cùng {@code /api/lessons}. Khác {@link LessonListItemResponse} ở chỗ có
 * {@code content} và {@link #getVocabularies()}. Ở đường quản trị hai trường
 * {@code isCompleted}/{@code completionPercentage} được đặt cứng vì không có
 * người dùng ngữ cảnh để tra tiến độ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {
    private Long id;
    private String title;
    private String description;
    private String content;
    private String level;
    private String category;
    private Integer durationMinutes;
    private String thumbnailUrl;
    private String audioUrl;
    private Integer orderIndex;
    
    private Boolean isCompleted;
    private BigDecimal completionPercentage;

    private List<VocabularyDTO> vocabularies;
}
