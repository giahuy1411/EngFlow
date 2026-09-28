package com.datn.engflow.model.dto.request;

import lombok.Data;

/**
 * DTO sinh bài tập hàng loạt cho toàn bộ bài học còn thiếu.
 *
 * <p>Hiện KHÔNG có endpoint nào bind lớp này: {@code POST
 * /api/admin/exercises/ai/generate-batch} nhận {@code force} qua
 * {@code @RequestParam(defaultValue = "false")} rồi chuyển thẳng cho
 * {@code AiExerciseService.generateBatch(force)}. Lớp được giữ lại như một trạng thái chờ —
 * khi thêm binding, {@link #force} mới thực sự được dùng.
 */
@Data
public class AiBatchGenerateRequest {
    /**
     * Khi {@code false}, {@code AiExerciseService.generateBatch} bỏ qua mọi bài học đã có
     * ít nhất một bài tập; khi {@code true} thì sinh lại cho tất cả bài học.
     */
    private boolean force = false;
}
