package com.datn.engflow.controller;

import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.service.AiVocabService;
import com.datn.engflow.service.UserService;
import jakarta.validation.Valid;
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

        // Quota: 5 lượt sinh AI miễn phí mỗi ngày cho tài khoản thường; premium/admin không giới hạn.
        User user = null;
        if (authentication != null && authentication.isAuthenticated() && userPrincipal != null) {
            user = userService.findEntityById(userPrincipal.getId());
            if (!userService.hasAiGenerationQuota(user)) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                        "Bạn đã dùng hết " + UserService.AI_GENERATIONS_PER_DAY
                                + " lượt sinh từ vựng AI miễn phí hôm nay. Đăng ký Premium để sinh không giới hạn.");
                problem.setTitle("AI Generation Quota Exceeded");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
            }
        }

        List<Vocabulary> generated = aiVocabService.generateVocabByTopic(topic, level, count).block();
        if (user != null) {
            userService.consumeAiGenerationQuota(user);
        }
        return ResponseEntity.ok(generated);
    }

    @PostMapping("/enrich-word")
    public ResponseEntity<?> enrichWord(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal com.datn.engflow.security.UserPrincipal userPrincipal,
            Authentication authentication) {
        String word = payload.get("word");
        if (word == null || word.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "word không được để trống"));
        }
        // audit-v6 F31: enrich-word hits the local LLM like generate-vocab —
        // apply the same free-tier quota so it can't be used to burn GPU.
        User user = null;
        if (authentication != null && authentication.isAuthenticated() && userPrincipal != null) {
            user = userService.findEntityById(userPrincipal.getId());
            if (!userService.hasAiGenerationQuota(user)) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                        "Bạn đã dùng hết " + UserService.AI_GENERATIONS_PER_DAY
                                + " lượt sinh từ vựng AI miễn phí hôm nay. Đăng ký Premium để dùng không giới hạn.");
                problem.setTitle("AI Generation Quota Exceeded");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
            }
        }
        // Đếm lượt SAU khi gọi AI thành công: lỗi phía dưới không được tính vào quota,
        // và premium/admin được bỏ qua bên trong consumeAiGenerationQuota.
        Vocabulary enriched = aiVocabService.enrichWord(word).block();
        if (user != null) {
            userService.consumeAiGenerationQuota(user);
        }
        return ResponseEntity.ok(enriched);
    }

    /**
     * Lưu loạt từ do AI sinh vào bảng từ vựng dùng chung.
     *
     * @param words danh sách từ đã sinh — mỗi phần tử được Bean Validation kiểm tra
     *              (chuỗi {@code @Valid} trên tham số không cascade vào phần tử List,
     *              phải đặt trong dấu ngoặc góc). Payload rác trả 400, không rơi xuống DB.
     * @return các từ đã lưu kèm id
     */
    @PostMapping("/save-vocab")
    public ResponseEntity<?> saveVocab(
            @RequestBody List<@Valid VocabularyRequest> words,
            @AuthenticationPrincipal com.datn.engflow.security.UserPrincipal userPrincipal) {
        // audit-v7 F60: trước đây payload bao nhiêu cũng nhận, không tốn quota →
        // sinh 50 từ (quota) rồi nổ batch ghi vocab toàn cục vô hạn. Cap 50 +
        // tính 1 lượt quota mỗi lần save cho user thường (premium/admin free).
        if (words == null || words.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Danh sách từ trống"));
        }
        if (words.size() > 50) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tối đa 50 từ mỗi lần lưu"));
        }
        // Endpoint đã bắt buộc authenticated ở SecurityConfig; userPrincipal null
        // chỉ xảy ra trong standalone test context (không có filter chain).
        if (userPrincipal != null) {
            User user = userService.findEntityById(userPrincipal.getId());
            if (!userService.hasAiGenerationQuota(user)) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                        "Bạn đã dùng hết lượt AI hôm nay. Đăng ký Premium để tiếp tục.");
                problem.setTitle("AI Generation Quota Exceeded");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
            }
            userService.consumeAiGenerationQuota(user);
        }
        return ResponseEntity.ok(aiVocabService.saveVocabBatch(words));
    }
}