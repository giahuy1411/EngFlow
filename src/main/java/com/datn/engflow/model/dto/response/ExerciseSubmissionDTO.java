package com.datn.engflow.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSubmissionDTO {
    private Long id;
    private Long exerciseId;
    private String userAnswer;
    private Boolean isCorrect;
    private Integer pointsEarned;
    private LocalDateTime submittedAt;
}
