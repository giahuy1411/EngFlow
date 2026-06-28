package com.datn.engflow.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonSkillDTO {
    private Long id;
    private String skillType;
    private String content;
}
