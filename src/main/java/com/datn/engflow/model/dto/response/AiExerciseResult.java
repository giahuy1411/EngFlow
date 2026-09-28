package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.entity.Exercise;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Kết quả sinh câu hỏi hàng loạt cho một bài học.
 *
 * <p>Tầng response của {@code POST /api/admin/exercises/ai/generate-all} (xem
 * {@link com.datn.engflow.controller.AdminAiExerciseController}). Mang cả entity
 * {@link Exercise} đã sinh để admin xem ngay, cùng số đếm valid/error và danh sách
 * lỗi chi tiết.
 */
@Data
@Builder
public class AiExerciseResult {
    private int generated;
    private int valid;
    private int errors;
    private List<Exercise> exercises;
    private List<String> errorDetails;
    private String researchSummary;
}
