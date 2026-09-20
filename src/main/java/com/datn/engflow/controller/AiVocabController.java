package com.datn.engflow.controller;

import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.service.AiVocabService;
import com.datn.engflow.service.DeckService;
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
    // audit-v11 F145: dùng lại addWordToDeck (đã kiểm quyền sở hữu + idempotent) để từ AI
    // sinh ra thực sự vào được một bộ từ người dùng mở lại được.
    private final DeckService deckService;

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
     * <p>audit-v11 F145: trước đây endpoint này chỉ ghi vào bảng {@code vocabulary} toàn cục với
     * {@code lesson_id = NULL} và không gắn vào deck nào — và {@code vocabulary} không có cột chủ
     * sở hữu. Nghĩa là người dùng trả 1 lượt quota, được báo "Đã lưu N từ vào DB!" và **không bao
     * giờ nhìn thấy những từ đó nữa**: chúng không nằm trong {@code /api/decks/my}, cũng không phải
     * của riêng họ ở bất kỳ đâu. Đo trên DB thật: 21/31 từ AI_GENERATED mồ côi, không thuộc deck nào.
     *
     * <p>Nay nhận thêm {@code deckId} tuỳ chọn; khi có, mỗi từ được thêm vào deck đó qua
     * {@link com.datn.engflow.service.DeckService#addWordToDeck} (đã kiểm quyền sở hữu + idempotent),
     * nên từ nằm ở nơi người dùng thực sự tới được. Khi không truyền deck, response nói rõ điều đó
     * thay vì ngụ ý từ đã sẵn dùng.
     *
     * @param words   danh sách từ đã sinh — mỗi phần tử được Bean Validation kiểm tra
     *                (chuỗi {@code @Valid} trên tham số không cascade vào phần tử List,
     *                phải đặt trong dấu ngoặc góc). Payload rác trả 400, không rơi xuống DB.
     * @param deckId  deck đích (tuỳ chọn) — phải thuộc sở hữu của người gọi
     * @return các từ đã lưu kèm id, và cho biết chúng đã vào deck nào
     */
    @PostMapping("/save-vocab")
    public ResponseEntity<?> saveVocab(
            @RequestBody List<@Valid VocabularyRequest> words,
            @RequestParam(name = "deckId", required = false) Long deckId,
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
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
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

        List<Vocabulary> saved = aiVocabService.saveVocabBatch(words);

        // audit-v11 F145: nếu có deck đích, gắn từng từ vào deck. addWordToDeck tự kiểm quyền sở
        // hữu (ném BadRequestException nếu không phải chủ) và bỏ qua từ đã có trong deck — nên nếu
        // request này trả 200 thì việc gắn deck CHẮC CHẮN đã thành công, không có thất bại im lặng.
        if (deckId != null && userId != null) {
            for (Vocabulary v : saved) {
                deckService.addWordToDeck(deckId, v.getId(), userId);
            }
        }

        // Giữ nguyên HỢP ĐỒNG cũ: body là mảng các từ đã lưu. (Bản nháp đầu của bản sửa này đổi
        // body thành object kèm metadata và làm đỏ AiVocabSaveVocabValidationTest — đổi shape của
        // một API đang chạy chỉ để thêm thông tin mà client không đọc là cái giá sai.)
        // Trạng thái "có vào deck hay không" đi qua header để client nào cần thì đọc.
        return ResponseEntity.ok()
                .header("X-AI-Saved-Count", String.valueOf(saved.size()))
                .header("X-AI-Linked-To-Deck", String.valueOf(deckId != null))
                .header("X-AI-Deck-Id", deckId != null ? String.valueOf(deckId) : "")
                .body(saved);
    }
}