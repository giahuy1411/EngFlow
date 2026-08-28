package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
/**
 * class GradeResponse.
 */
public class GradeResponse {
    private List<ExerciseGradeItem> results;
    private int score;
    private int total;
    private double percentage;
}
