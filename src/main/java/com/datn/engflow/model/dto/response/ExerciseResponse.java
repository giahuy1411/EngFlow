package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Một câu hỏi trả về cho client, dùng chung cho cả đường học và đường quản trị.
 *
 * <p>Kiểu response của {@code /api/lessons/{lessonId}/exercises} và
 * {@code /api/admin/exercises}. Với đường học, {@code correctAnswer} và
 * {@code explanation} chỉ được điền khi đường đó cho phép xem đáp án; với bảng
 * quản trị thì luôn có đủ. Nhờ vậy client đọc một shape duy nhất.
 */
@Data
@Builder
public class ExerciseResponse {
    private Long id;
    private Long lessonId;
    private String lessonTitle;
    private String question;
    private String options;
    private String correctAnswer;
    private String exerciseType;
    private String difficulty;
    private String explanation;
    private String imageUrl;
    private String audioUrl;
    private Integer orderIndex;
}
