package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.entity.Exercise;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
/**
 * class AiExerciseResult.
 */
public class AiExerciseResult {
    private int generated;
    private int valid;
    private int errors;
    private List<Exercise> exercises;
    private List<String> errorDetails;
    private String researchSummary;
}
