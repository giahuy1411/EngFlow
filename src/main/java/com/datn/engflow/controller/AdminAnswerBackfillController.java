package com.datn.engflow.controller;

import com.datn.engflow.service.AiAnswerBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint admin điền bù (backfill) các giá trị {@code correct_answer} còn trống cho bài tập seed:
 * lớp parse answer-key tất định chạy trước, phần dư mới giao cho Ollama.
 *
 * <p>Chạy bất đồng bộ: trả HTTP 202 kèm {@code batchId} rồi client poll trạng thái — cùng khuôn với
 * luồng sinh bài tập bằng AI.</p>
 *
 * <p><b>Trùng base path có chủ đích:</b> controller này dùng chung {@code /api/admin/exercises/ai}
 * với {@code AdminAiExerciseController}. Không xung đột vì đường dẫn method khác nhau
 * ({@code /backfill-answers...} so với {@code /generate...}); Spring chỉ nổ {@code Ambiguous mapping}
 * khi trùng cả method HTTP lẫn path, nên đây là thiết kế hợp lệ chứ không phải lỗi.</p>
 *
 * <p><b>Bảo vệ:</b> toàn bộ {@code /api/admin/**} đã bị {@code SecurityConfig} chặn bằng
 * {@code hasRole('ADMIN')}; {@code @PreAuthorize} cấp class là lớp chặn thứ hai (defense-in-depth)
 * phòng khi rule URL bị đổi về sau.</p>
 */
@RestController
@RequestMapping("/api/admin/exercises/ai")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnswerBackfillController {

    private final AiAnswerBackfillService backfillService;

    /**
     * Khởi động một lượt backfill.
     *
     * <p>Nếu đang có lượt chạy dở, trả ngay HTTP 409 kèm {@code checkpointLessonId} hiện tại thay vì
     * mở lượt thứ hai — chỉ một pipeline chạy tại một thời điểm.</p>
     *
     * @param dryRun   true → chạy pipeline nhưng bỏ qua ghi DB; dùng để đo tỉ lệ điền được trước.
     * @param limit    số bài tập tối đa xử lý trong lượt này (ngân sách theo cả lesson); 0 = tất cả.
     * @param restart  true → bỏ qua checkpoint lesson và chạy lại từ đầu.
     * @param lessonId khi được set, giới hạn lượt chạy trong đúng lesson đó và KHÔNG đụng tới
     *                 checkpoint bền (dùng để chứng minh / đo tỉ lệ điền theo từng lesson).
     * @param mode     "full" (mặc định) → chạy các lớp answer-key rồi tới phần dư Ollama;
     *                 "deterministic" → chỉ các lớp answer-key, không bao giờ gọi lớp AI
     *                 (gate 3.4-B: một key AI chưa kiểm chứng chấm còn tệ hơn key trống — đo được
     *                 key rác {@code "1. a"} trên một gap ngữ pháp).
     * @return HTTP 202 kèm {@code batchId} + cấu hình lượt chạy, hoặc HTTP 409 nếu đang chạy
     */
    @PostMapping("/backfill-answers")
    public ResponseEntity<Map<String, Object>> startBackfill(
            @RequestParam(defaultValue = "false") boolean dryRun,
            @RequestParam(defaultValue = "0") int limit,
            @RequestParam(defaultValue = "false") boolean restart,
            @RequestParam(required = false) Long lessonId,
            @RequestParam(defaultValue = "full") String mode) {
        boolean deterministicOnly = "deterministic".equalsIgnoreCase(mode);
        if (backfillService.isRunning()) {
            return ResponseEntity.status(409).body(Map.of(
                    "status", "already-running",
                    "checkpointLessonId", backfillService.getCheckpointLessonId()));
        }
        String batchId = backfillService.startBackfill(dryRun, limit, restart, lessonId, deterministicOnly);
        return ResponseEntity.accepted().body(Map.of(
                "batchId", batchId,
                "status", "started",
                "dryRun", dryRun,
                "limit", limit,
                "lessonId", lessonId == null ? 0L : lessonId,
                "mode", deterministicOnly ? "deterministic" : "full",
                "checkpointLessonId", backfillService.getCheckpointLessonId()));
    }

    /**
     * Poll một lượt backfill (cùng hình dạng với endpoint trạng thái của luồng sinh AI).
     *
     * <p>Không truyền {@code batchId} hoặc batch đã xong/bị quên → trả {@code running=false} kèm
     * {@code checkpointLessonId} hiện tại, KHÔNG báo lỗi: client chỉ cần biết "hết việc". Nhánh này
     * cố tình trả 200 để vòng poll của frontend kết thúc sạch.</p>
     *
     * @param batchId định danh lượt chạy cần tra; null ⇒ coi như không còn gì đang chạy
     * @return tiến độ chi tiết (đếm bài, số điền được, lỗi, mẫu không điền được…) dạng JSON
     */
    @GetMapping("/backfill-answers/status")
    public ResponseEntity<Map<String, Object>> getBackfillStatus(
            @RequestParam(name = "batchId", required = false) String batchId) {
        AiAnswerBackfillService.BackfillProgress p = batchId != null
                ? backfillService.getProgress(batchId) : null;
        if (p == null) {
            return ResponseEntity.ok(Map.of(
                    "running", false,
                    "checkpointLessonId", backfillService.getCheckpointLessonId()));
        }
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("running", p.running);
        body.put("dryRun", p.dryRun);
        body.put("totalExercises", p.totalExercises);
        body.put("processed", p.processed);
        body.put("backfilled", p.backfilled);
        body.put("deterministic", p.deterministic);
        body.put("aiFilled", p.aiFilled);
        body.put("unfillable", p.unfillable);
        body.put("errors", p.errors);
        body.put("currentLesson", p.currentLesson == null ? "" : p.currentLesson);
        body.put("checkpointLessonId", p.checkpointLessonId);
        body.put("elapsedMs", p.elapsedMs);
        body.put("errorDetails", p.errorDetails);
        body.put("unfillableSamples", p.unfillableSamples);
        return ResponseEntity.ok(body);
    }
}
