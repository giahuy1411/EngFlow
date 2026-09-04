package com.datn.engflow.controller.speaking;

import com.datn.engflow.model.dto.request.CreateSpeakingPromptRequest;
import com.datn.engflow.model.dto.response.SpeakingPromptResponse;
import com.datn.engflow.service.AiPromptService;
import com.datn.engflow.service.SpeakingPromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
/**
 * class SpeakingPromptController.
 */
public class SpeakingPromptController {

    private final SpeakingPromptService SpeakingPromptService;
    private final AiPromptService aiPromptService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping({"/api/v1/admin/speaking-prompts", "/api/v1/admin/video-prompts"})
    public ResponseEntity<Page<SpeakingPromptResponse>> getAllPromptsForAdmin(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = promptPageRequest(page, size);
        return ResponseEntity.ok(SpeakingPromptService.getAllPromptsForAdmin(q, pageable).map(SpeakingPromptResponse::from));
    }

    @GetMapping(value = {"/api/v1/speaking-prompts", "/api/v1/video-prompts"}, params = "q")
    public ResponseEntity<Page<SpeakingPromptResponse>> getAllPrompts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = promptPageRequest(page, size);
        return ResponseEntity.ok(SpeakingPromptService.getAllPrompts(q, pageable).map(SpeakingPromptResponse::from));
    }

    @GetMapping(value = {"/api/v1/speaking-prompts", "/api/v1/video-prompts"}, params = "!q")
    public ResponseEntity<Page<SpeakingPromptResponse>> getAllPrompts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return getAllPrompts((String) null, page, size);
    }

    @GetMapping({"/api/v1/speaking-prompts/{id}", "/api/v1/video-prompts/{id}"})
    public ResponseEntity<SpeakingPromptResponse> getPrompt(@PathVariable Long id) {
        return ResponseEntity.ok(SpeakingPromptResponse.from(SpeakingPromptService.getPrompt(id)));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({"/api/v1/admin/speaking-prompts", "/api/v1/admin/video-prompts"})
    public ResponseEntity<SpeakingPromptResponse> createPrompt(
            @Valid @RequestBody CreateSpeakingPromptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SpeakingPromptResponse.from(SpeakingPromptService.createPrompt(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping({"/api/v1/admin/speaking-prompts/{id}", "/api/v1/admin/video-prompts/{id}"})
    public ResponseEntity<SpeakingPromptResponse> updatePrompt(
            @PathVariable Long id,
            @Valid @RequestBody CreateSpeakingPromptRequest request) {
        return ResponseEntity.ok(SpeakingPromptResponse.from(SpeakingPromptService.updatePrompt(id, request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping({"/api/v1/admin/speaking-prompts/{id}", "/api/v1/admin/video-prompts/{id}"})
    public ResponseEntity<Map<String, String>> deletePrompt(@PathVariable Long id) {
        SpeakingPromptService.deletePrompt(id);
        return ResponseEntity.ok(Map.of("message", "Prompt deleted successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({"/api/v1/admin/speaking-prompts/ai-generate", "/api/v1/admin/video-prompts/ai-generate"})
    public ResponseEntity<Map<String, String>> aiGeneratePrompt(
            @RequestBody Map<String, String> body) {
        String topic = body.getOrDefault("topic", "");
        String level = body.getOrDefault("level", "B1");
        // Blocking call with timeout: a Mono return value triggers an async error
        // redispatch that clears the SecurityContext and masks real failures as 401.
        String result;
        try {
            result = aiPromptService.generateSpeakingPrompt(topic, level)
                    .timeout(java.time.Duration.ofSeconds(60))
                    .block(java.time.Duration.ofSeconds(65));
        } catch (Exception ex) {
            log.error("AI prompt generation failed for topic '{}': {}", topic, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "AI không phản hồi"));
        }
        if (result == null || result.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "AI không phản hồi"));
        }
        return ResponseEntity.ok(Map.of(
                "content", result,
                "referenceText", aiPromptService.extractReferenceText(result),
                "topic", topic));
    }

    /**
     * Full AI draft for the "tạo đề luyện nói bằng AI" button: fills
     * title/description/prompt/referenceText/level in one LLM call.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({"/api/v1/admin/speaking-prompts/ai-generate-full", "/api/v1/admin/video-prompts/ai-generate-full"})
    public ResponseEntity<Map<String, String>> aiGenerateFullPrompt(
            @RequestBody Map<String, String> body) {
        String topic = body.getOrDefault("topic", "");
        if (topic.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cần nhập chủ đề"));
        }
        try {
            return ResponseEntity.ok(aiPromptService.generateFullPrompt(
                    topic, body.get("level"), body.get("mode")));
        } catch (Exception ex) {
            log.error("AI full prompt generation failed for topic '{}': {}", topic, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "AI không phản hồi — thử lại sau"));
        }
    }

    private PageRequest promptPageRequest(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by("orderIndex").ascending().and(Sort.by("id")));
    }
}
