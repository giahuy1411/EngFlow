package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Báo cáo kiểm tra một lô câu hỏi trước khi lưu.
 *
 * <p>Tầng response của {@code POST /api/admin/exercises/ai/validate}. Chạy hai lớp
 * kiểm tra: schema (do {@code AiExerciseService.validateSchema} quyết định) và
 * review bằng AI (tuỳ chọn theo cờ {@code useAiReview}); mỗi lớp có một list kết
 * quả riêng để admin đối chiếu với từng câu.
 */
@Data
@Builder
public class AiValidateResult {
    private int total;
    private int valid;
    private int invalid;
    private List<SchemaResult> schemaResults;
    private List<AiReview> aiReviews;

    /**
     * Kết quả kiểm tra schema của một câu hỏi.
     *
     * @param valid true khi {@code AiExerciseService.validateSchema} trả về null lỗi
     * @param question nội dung câu hỏi được kiểm tra, dùng để admin đối chiếu
     * @param error chuỗi mô tả lỗi; null khi {@code valid} là true
     */
    @Data
    @Builder
    public static class SchemaResult {
        private boolean valid;
        private String question;
        private String error;
    }

    /**
     * Kết quả review bằng AI của một câu hỏi hợp lệ về schema.
     *
     * @param question nội dung câu hỏi được review
     * @param passed true khi AI cho rằng câu hỏi đạt
     * @param reason lý do AI nêu ra, thường là lý do trượt
     */
    @Data
    @Builder
    public static class AiReview {
        private String question;
        private boolean passed;
        private String reason;
    }
}
