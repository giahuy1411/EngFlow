package com.datn.engflow.model.dto.request;

import lombok.Data;
import java.util.List;

@Data
/**
 * class GradeRequest.
 */
public class GradeRequest {
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long exerciseId;
        private String userAnswer;
    }
}
