package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.AiGenerateRequest;
import com.datn.engflow.model.dto.request.AiValidateRequest;
import com.datn.engflow.model.dto.response.AiExerciseResult;
import com.datn.engflow.model.dto.response.AiValidateResult;
import com.datn.engflow.model.dto.response.BatchGenerateStatus;
import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.service.AiExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cổng REST cho sinh bài tập bằng AI: chạy batch không chặn, sinh cho cả bài, sinh theo lô,
 * kiểm tra schema/bản AI cho bản nháp và tra tiến độ.
 *
 * <p>Tầng controller — {@link AiExerciseService} giữ toàn bộ logic gọi LLM và quản lý batch;
 * controller chỉ tra lesson qua {@link LessonRepository} rồi chuyển tiếp. Toàn bộ path
 * {@code /api/admin/**} đã bị {@code SecurityConfig} chặn; {@code @PreAuthorize} ở đây là
 * lớp chặn thứ hai cho các path AI.
 *
 * <p>Hợp đồng chung: các endpoint sinh trả HTTP 202 kèm {@code batchId}, client poll
 * {@code /status} thay vì chờ.
 */
@RestController
@RequestMapping("/api/admin/exercises/ai")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiExerciseController {

    private final AiExerciseService aiExerciseService;
    private final LessonRepository lessonRepository;

    /**
     * Sinh bài tập cho một bài, trả về ngay mà không chờ LLM.
     *
     * @param request id bài, số câu (mặc định 5) và loại bài tập tuỳ chọn
     * @return body chứa {@code batchId} và {@code status=started}, HTTP 202
     * @throws com.datn.engflow.exception.ResourceNotFoundException khi id bài không tồn tại
     */
    @PostMapping("/generate-async")
    public ResponseEntity<Map<String, String>> generateExercisesAsync(@Valid @RequestBody AiGenerateRequest request) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new com.datn.engflow.exception.ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        int count = request.getCount() != null ? request.getCount() : 5;
        ExerciseType type = (request.getExerciseType() == null || request.getExerciseType().isBlank())
                ? null : ExerciseType.valueOf(request.getExerciseType().toUpperCase());
        // Non-blocking: async generation returns a batchId immediately (HTTP 202 Accepted).
        String batchId = aiExerciseService.generateSingleAsync(lesson, count, type);
        return ResponseEntity.accepted()
                .body(Map.of("batchId", batchId, "status", "started"));
    }

    /**
     * Sinh bài tập cho một bài (đường đồng bộ-như-async, giữ để tương thích client cũ).
     *
     * <p>Hành vi giống hệt {@link #generateExercisesAsync} — cùng gọi
     * {@code generateSingleAsync} và trả 202 + {@code batchId} ngay, client poll
     * {@code /status} để theo dõi. Chỉ khác đường dẫn {@code /generate}.</p>
     *
     * @param request id bài, số câu (mặc định 5) và loại bài tập tuỳ chọn
     * @return body chứa {@code batchId} và {@code status=started}, HTTP 202
     * @throws com.datn.engflow.exception.ResourceNotFoundException khi id bài không tồn tại
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateExercises(@Valid @RequestBody AiGenerateRequest request) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new com.datn.engflow.exception.ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        int count = request.getCount() != null ? request.getCount() : 5;
        ExerciseType type = (request.getExerciseType() == null || request.getExerciseType().isBlank())
                ? null : ExerciseType.valueOf(request.getExerciseType().toUpperCase());
        String batchId = aiExerciseService.generateSingleAsync(lesson, count, type);
        return ResponseEntity.accepted()
                .body(Map.of("batchId", batchId, "status", "started"));
    }

    /**
     * Sinh toàn bộ bài tập cho một bài trong MỘT lần gọi và trả kết quả NGAY (blocking).
     *
     * <p>Khác {@code /generate-async}: chờ LLM sinh xong rồi trả thẳng danh sách bài tập vừa
     * sinh (200). Vì blocking nên chỉ dùng cho lô nhỏ; lô lớn dùng đường async.</p>
     *
     * @param request id bài và số câu (mặc định 5)
     * @return {@link AiExerciseResult} gồm số bài sinh được và danh sách bài tập
     * @throws com.datn.engflow.exception.ResourceNotFoundException khi id bài không tồn tại
     */
    @PostMapping("/generate-all")
    public ResponseEntity<AiExerciseResult> generateAllForLesson(@Valid @RequestBody AiGenerateRequest request) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new com.datn.engflow.exception.ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        int count = request.getCount() != null ? request.getCount() : 5;
        List<Exercise> exercises = aiExerciseService.generateAll(lesson, count);
        return ResponseEntity.ok(AiExerciseResult.builder()
                .generated(exercises.size())
                .valid(exercises.size())
                .errors(0)
                .exercises(exercises)
                .errorDetails(List.of())
                .build());
    }

    /**
     * Sinh bài tập hàng loạt cho NHIỀU bài trong một lần chạy nền.
     *
     * <p>Tiến độ lưu in-memory (ConcurrentHashMap, single-container) và tra qua
     * {@code /status}. Tham số {@code force} quyết định có sinh đè cả những bài đã có bài tập
     * hay bỏ qua chúng.</p>
     *
     * @param force true = sinh lại kể cả bài đã có bài tập; mặc định false
     * @return {@link BatchGenerateStatus} phản ánh tiến độ batch vừa khởi động
     */
    @PostMapping("/generate-batch")
    public ResponseEntity<BatchGenerateStatus> generateBatch(@RequestParam(defaultValue = "false") boolean force) {
        AiExerciseService.BatchProgress progress = aiExerciseService.generateBatch(force);
        return ResponseEntity.ok(BatchGenerateStatus.builder()
                .totalLessons(progress.totalLessons)
                .processed(progress.processed)
                .generated(progress.generated)
                .errors(progress.errors)
                .running(progress.running)
                .currentLesson(progress.currentLesson)
                .build());
    }

    /**
     * Kiểm tra một danh sách bài tập nháp: validate schema từng bài, tuỳ chọn thêm AI review.
     *
     * <p>KHÔNG ghi DB — chỉ trả báo cáo để admin duyệt trước khi lưu. Với mỗi bài, schema được
     * kiểm qua {@code validateSchema}; nếu request bật {@code useAiReview} và bài hợp lệ thì
     * gọi thêm {@code reviewExercise} (tốn thêm một lượt LLM/bài).</p>
     *
     * @param request danh sách bài nháp và cờ {@code useAiReview}
     * @return {@link AiValidateResult} gồm số bài hợp lệ/không, kết quả schema và AI review
     */
    @PostMapping("/validate")
    public ResponseEntity<AiValidateResult> validateExercises(@Valid @RequestBody AiValidateRequest request) {
        List<AiValidateResult.SchemaResult> schemaResults = new ArrayList<>();
        List<AiValidateResult.AiReview> aiReviews = new ArrayList<>();
        int validCount = 0;
        for (AiValidateRequest.ExerciseDraft draft : request.getExercises()) {
            Exercise ex = new Exercise();
            ex.setQuestion(draft.getQuestion());
            ex.setOptions(draft.getOptions());
            ex.setCorrectAnswer(draft.getCorrectAnswer());
            ex.setExplanation(draft.getExplanation());
            try {
                ex.setExerciseType(ExerciseType.valueOf(draft.getExerciseType().toUpperCase()));
            } catch (Exception e) {
                ex.setExerciseType(ExerciseType.MULTIPLE_CHOICE);
            }
            String error = aiExerciseService.validateSchema(ex);
            boolean isValid = error == null;
            if (isValid) validCount++;
            schemaResults.add(AiValidateResult.SchemaResult.builder()
                    .valid(isValid)
                    .question(draft.getQuestion())
                    .error(error)
                    .build());
            if (request.isUseAiReview() && isValid) {
                AiExerciseService.ReviewResult review = aiExerciseService.reviewExercise(ex);
                aiReviews.add(AiValidateResult.AiReview.builder()
                        .question(draft.getQuestion())
                        .passed(review.passed)
                        .reason(review.reason)
                        .build());
            }
        }
        return ResponseEntity.ok(AiValidateResult.builder()
                .total(request.getExercises().size())
                .valid(validCount)
                .invalid(request.getExercises().size() - validCount)
                .schemaResults(schemaResults)
                .aiReviews(aiReviews)
                .build());
    }

    /**
     * Tra tiến độ một batch sinh bài (client poll sau khi nhận 202).
     *
     * <p>Nếu có {@code batchId} thì tra đúng batch đó; không thì lấy batch gần nhất
     * ({@code getBatchProgress}). Batch không tồn tại/đã dọn → trả trạng thái rỗng
     * {@code running=false} thay vì 404, để client poll không cần phân nhánh lỗi.</p>
     *
     * @param batchId id batch cần tra, tuỳ chọn
     * @return {@link BatchGenerateStatus} gồm tiến độ, bài đã sinh, chi tiết lỗi
     */
    @GetMapping("/status")
    public ResponseEntity<BatchGenerateStatus> getBatchStatus(@RequestParam(name = "batchId", required = false) String batchId) {
        AiExerciseService.BatchProgress progress = (batchId != null)
                ? aiExerciseService.getProgress(batchId)
                : aiExerciseService.getBatchProgress();
        if (progress == null) {
            return ResponseEntity.ok(BatchGenerateStatus.builder()
                    .running(false)
                    .totalLessons(0)
                    .processed(0)
                    .generated(0)
                    .errors(0)
                    .currentLesson("")
                    .batchId(batchId != null ? batchId : "")
                    .build());
        }
        return ResponseEntity.ok(BatchGenerateStatus.builder()
                .totalLessons(progress.totalLessons)
                .processed(progress.processed)
                .generated(progress.generated)
                .errors(progress.errors)
                .running(progress.running)
                .currentLesson(progress.currentLesson)
                .batchId(batchId != null ? batchId : "")
                .exercises(progress.exercises)
                .errorDetails(progress.errorDetails)
                .build());
    }
}
