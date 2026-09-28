package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.entity.Exercise;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Trạng thái tiến trình sinh câu hỏi hàng loạt.
 *
 * <p>Tầng response của {@code /api/admin/exercises/ai/generate-batch} và
 * {@code /status}: {@code AdminAiExerciseController} map thẳng từ
 * {@code AiExerciseService.BatchProgress}, nên tên trường khớp 1-1 với bản ghi
 * tiến trình phía service. Dùng cho cả lúc chạy ({@code running=true}) và sau khi
 * kết thúc.
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
