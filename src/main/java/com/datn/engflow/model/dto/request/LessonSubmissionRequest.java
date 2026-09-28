package com.datn.engflow.model.dto.request;

import com.datn.engflow.model.enums.SkillType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO nộp bài theo kỹ năng của một bài học, bind từ body của
 * {@code POST /api/lesson-submissions/submit} và được
 * {@code LessonSubmissionService.submitLessonSkill} xử lý.
 *
 * <p>Khóa duy nhất của một bài nộp là bộ ba user + lesson + {@code skillType}: nếu đã có
 * bản ghi cho bộ ba đó thì service ghi đè nội dung, xoá điểm và đưa trạng thái về
 * {@code PENDING} để chấm lại, thay vì tạo bản ghi mới.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonSubmissionRequest {

    @NotNull(message = "Mã bài học không được để trống")
    private Long lessonId;

    @NotNull(message = "Loại kỹ năng không được để trống")
    private SkillType skillType;

    private String submissionText;

    private String audioUrl;
}
