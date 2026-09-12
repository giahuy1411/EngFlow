package com.datn.engflow.controller;

import com.datn.engflow.service.AiAnswerBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin endpoint for backfilling empty correct_answer values on seed
 * exercises (deterministic answer-key parse first, Ollama residue second).
 * Async 202 + batchId polling, mirroring the existing AI generation flow.
 */
@RestController
@RequestMapping("/api/admin/exercises/ai")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnswerBackfillController {

    private final AiAnswerBackfillService backfillService;

    /**
     * Starts a backfill run.
     *
     * @param dryRun   true → run the pipeline but skip persistence; used to measure
     *                 the fill rate first.
     * @param limit    max exercises to process this run (whole-lesson budget); 0 = all.
     * @param restart  true → ignore the lesson checkpoint and start over.
     * @param lessonId when set, restrict the run to that single lesson and leave
     *                 the durable checkpoint untouched (proof / per-lesson fill).
     * @param mode     "full" (default) → answer-key layers then Ollama residue;
     *                 "deterministic" → answer-key layers only, never call the AI
     *                 layer (gate 3.4-B: an unverified AI key grades worse than an
     *                 empty one — measured "1. a" garbage on a grammar gap).
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

    /** Poll a backfill run (same shape as the AI generation status endpoint). */
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
