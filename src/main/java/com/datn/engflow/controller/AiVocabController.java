package com.datn.engflow.controller;

import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.service.AiVocabService;
import com.datn.engflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
/**
 * class AiVocabController.
 */
public class AiVocabController {

    private final AiVocabService aiVocabService;
    private final UserService userService;

    @PostMapping("/generate-vocab")
    public ResponseEntity<?> generateVocab(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal com.datn.engflow.security.UserPrincipal userPrincipal,
            Authentication authentication) {
        String topic = payload.get("topic");
        if (topic == null || topic.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "topic không được để trống"));
        }
        String level = payload.getOrDefault("level", "B2");
        int count;
        try {
            count = Integer.parseInt(payload.getOrDefault("count", "10"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "count phải là số nguyên"));
        }
        if (count < 1 || count > 50) {
            return ResponseEntity.badRequest().body(Map.of("error", "count phải từ 1 đến 50"));
        }

        // Quota: 5 generations for free users; premium/admin unlimited.
        User user = null;
        if (authentication != null && authentication.isAuthenticated() && userPrincipal != null) {
            user = userService.findEntityById(userPrincipal.getId());
            if (!userService.hasUnlimitedAiGeneration(user)
                    && !aiVocabService.hasAiGenerationQuota(user)) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                        "Bạn đã dùng hết 5 lần sinh từ vựng AI miễn phí. Đăng ký Premium để sinh không giới hạn.");
                problem.setTitle("AI Generation Quota Exceeded");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
            }
        }

        List<Vocabulary> generated = aiVocabService.generateVocabByTopic(topic, level, count).block();
        if (user != null && !userService.hasUnlimitedAiGeneration(user)) {
            aiVocabService.incrementAiGenerationQuota(user);
        }
        return ResponseEntity.ok(generated);
    }

    @PostMapping("/enrich-word")
    public ResponseEntity<?> enrichWord(@RequestBody Map<String, String> payload) {
        String word = payload.get("word");
        if (word == null || word.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "word không được để trống"));
        }
        return ResponseEntity.ok(aiVocabService.enrichWord(word).block());
    }

    @PostMapping("/save-vocab")
    public ResponseEntity<List<Vocabulary>> saveVocab(@RequestBody List<VocabularyRequest> words) {
        return ResponseEntity.ok(aiVocabService.saveVocabBatch(words));
    }
}