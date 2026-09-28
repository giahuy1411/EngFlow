package com.datn.engflow.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận dữ liệu một từ vựng, dùng chung cho ba entry point: quản trị
 * ({@code POST/PUT /api/admin/vocabulary}), tra cứu của người dùng
 * ({@code POST /api/vocabulary}) và lưu hàng loạt từ do AI sinh
 * ({@code POST /api/ai/save-vocab}, body là {@code List<@Valid VocabularyRequest>}).
 *
 * <p>{@code AdminService.createVocabulary} và {@code updateVocabulary} chuyển nó thành
 * entity {@code Vocabulary}; ở endpoint AI, {@code AiVocabController} tự cap tối đa 50 từ
 * mỗi lần gọi trước khi ghi.
 *
 * <p>{@code lessonId} là con trỏ tuỳ chọn, không phải bắt buộc: {@code AdminService} chỉ
 * tìm bài học khi nó khác null, và {@code updateVocabulary} coi null là gỡ liên kết.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyRequest {

    private Long lessonId;

    @NotBlank(message = "Từ vựng không được để trống")
    @Size(max = 100, message = "Từ vựng tối đa 100 ký tự")
    private String word;

    @Size(max = 100, message = "Phát âm tối đa 100 ký tự")
    private String pronunciation;

    @NotBlank(message = "Nghĩa không được để trống")
    @Size(max = 500, message = "Nghĩa tối đa 500 ký tự")
    private String meaning;

    private String exampleSentence;

    @Size(max = 500, message = "Audio URL tối đa 500 ký tự")
    private String audioUrl;

    @Size(max = 500, message = "Image URL tối đa 500 ký tự")
    private String imageUrl;

    @Size(max = 50, message = "Loại từ tối đa 50 ký tự")
    private String wordType;

    private String definitionEn;

    @Size(max = 10, message = "CEFR level tối đa 10 ký tự")
    private String cefrLevel;

    @Size(max = 50, message = "Nguồn tối đa 50 ký tự")
    private String source;
}
