package com.datn.engflow.model.dto.response;

import lombok.*;

/**
 * Một từ vựng kèm ngữ cảnh, hiện chỉ dùng trong phần từ vựng của bài học.
 *
 * <p>Tầng response: {@code LessonService} nhúng danh sách DTO này vào
 * {@link LessonResponse#getVocabularies()} khi mở chi tiết bài học — cả đường
 * khách (chưa đăng nhập) lẫn đường đã đăng nhập đều map từ
 * {@code lesson.getVocabularies()}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularyDTO {
    private Long id;
    private String word;
    private String pronunciation;
    private String meaning;
    private String exampleSentence;
    private String audioUrl;
    private String imageUrl;
    private String wordType;
}
