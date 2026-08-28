package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
/**
 * class AiValidateResult.
 */
public class AiValidateResult {
    private int total;
    private int valid;
    private int invalid;
    private List<SchemaResult> schemaResults;
    private List<AiReview> aiReviews;

    @Data
    @Builder
    public static class SchemaResult {
        private boolean valid;
        private String question;
        private String error;
    }

    @Data
    @Builder
    public static class AiReview {
        private String question;
        private boolean passed;
        private String reason;
    }
}
