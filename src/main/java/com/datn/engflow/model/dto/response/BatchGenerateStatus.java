package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.entity.Exercise;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * class BatchGenerateStatus.
 */
@Data
@Builder
public class BatchGenerateStatus {
    private int totalLessons;
    private int processed;
    private int generated;
    private int errors;
    private boolean running;
    private String currentLesson;
    private String batchId;
    private List<Exercise> exercises;
    private List<String> errorDetails;
}
