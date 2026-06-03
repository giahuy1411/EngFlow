package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.enums.ExerciseType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseDTO {
    private Long id;
    private String title;
    private String question;
    private ExerciseType exerciseType;
    private String options;
    private Integer points;
    private String difficulty;
    private String audioUrl;
}
