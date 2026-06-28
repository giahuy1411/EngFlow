package com.datn.engflow.model.dto.request;

import com.datn.engflow.model.enums.SkillType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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
