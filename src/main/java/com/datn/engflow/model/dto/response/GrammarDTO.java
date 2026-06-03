package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.enums.LessonLevel;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrammarDTO {
    private Long id;
    private String title;
    private String explanation;
    private String formula;
    private String examples;
    private LessonLevel level;
}
