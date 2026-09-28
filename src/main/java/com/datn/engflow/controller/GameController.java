package com.datn.engflow.controller;

import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
/**
 * class GameController.
 */
public class GameController {

    private final GameService gameService;

    /**
     * Sinh đề trắc nghiệm (QUIZ) cho một deck: mỗi từ là 1 câu 4 lựa chọn (nghĩa đúng + tối đa
     * 3 nghĩa nhiễu), xáo thứ tự rồi cắt còn tối đa 10 câu.
     *
     * <p>Đề được lưu tạm ở Redis kèm answerMap để server tự chấm khi nộp; response trả
     * {@code sessionId} + {@code data}. Yêu cầu đăng nhập (kiểm thủ công vì principal có thể
     * null nếu thiếu token) — trả 401 khi chưa xác thực.</p>
     *
     * @param deckId        id deck nguồn
     * @param userPrincipal người chơi hiện tại
     * @return map {@code {sessionId, data}}, hoặc 401 nếu chưa đăng nhập
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu deck không tồn tại
     */
    @GetMapping("/quiz/{deckId}")
    public ResponseEntity<?> getQuizGame(@PathVariable Long deckId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gameService.generateQuiz(deckId, userPrincipal.getId()));
    }

    /**
     * Sinh bộ thẻ lật (MEMORY_MATCH): chọn tối đa 8 cặp từ↔nghĩa, mỗi cặp 2 thẻ, xáo trộn.
     *
     * @param deckId        id deck nguồn
     * @param userPrincipal người chơi hiện tại
     * @return map {@code {sessionId, data}} (data là danh sách thẻ), hoặc 401 nếu chưa đăng nhập
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu deck không tồn tại
     */
    @GetMapping("/memory/{deckId}")
    public ResponseEntity<?> getMemoryGame(@PathVariable Long deckId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gameService.generateMemoryMatch(deckId, userPrincipal.getId()));
    }

    /**
     * Sinh đề gõ lại từ (TYPING): hiện từ, người chơi gõ nghĩa; tối đa 10 câu.
     *
     * @param deckId        id deck nguồn
     * @param userPrincipal người chơi hiện tại
     * @return map {@code {sessionId, data}}, hoặc 401 nếu chưa đăng nhập
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu deck không tồn tại
     */
    @GetMapping("/typing/{deckId}")
    public ResponseEntity<?> getTypingGame(@PathVariable Long deckId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gameService.generateTyping(deckId, userPrincipal.getId()));
    }

    /**
     * Sinh đề nghe hiểu (LISTENING): phát âm từ, người chơi chọn/gõ nghĩa; tối đa 10 câu.
     *
     * @param deckId        id deck nguồn
     * @param userPrincipal người chơi hiện tại
     * @return map {@code {sessionId, data}}, hoặc 401 nếu chưa đăng nhập
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu deck không tồn tại
     */
    @GetMapping("/listening/{deckId}")
    public ResponseEntity<?> getListeningGame(@PathVariable Long deckId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gameService.generateListening(deckId, userPrincipal.getId()));
    }

    /**
     * Sinh đề tổng hợp (MIXED): trộn các dạng câu của nhiều game; tối đa 10 câu.
     *
     * @param deckId        id deck nguồn
     * @param userPrincipal người chơi hiện tại
     * @return map {@code {sessionId, data}}, hoặc 401 nếu chưa đăng nhập
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu deck không tồn tại
     */
    @GetMapping("/mixed/{deckId}")
    public ResponseEntity<?> getMixedGame(@PathVariable Long deckId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gameService.generateMixed(deckId, userPrincipal.getId()));
    }

    /**
     * Nộp kết quả một phiên chơi để cộng điểm.
     *
     * <p>Có 2 đường, ưu tiên đường an toàn: nếu client gửi {@code answers} (danh sách câu trả
     * lời chi tiết) thì server tự chấm lại theo answerMap trong phiên Redis, KHÔNG tin
     * {@code correctAnswers} client khai; nếu không có {@code answers} thì dùng
     * {@code correctAnswers} (vẫn bị chặn trần ngày để chống farm điểm).</p>
     *
     * <p>Validate đầu vào: {@code sessionId} bắt buộc là chuỗi không rỗng, {@code answers} nếu
     * có phải là danh sách các object, {@code correctAnswers} nếu có phải là số.</p>
     *
     * @param userPrincipal người chơi hiện tại
     * @param payload       body chứa {@code sessionId}, tuỳ chọn {@code answers} / {@code correctAnswers}
     * @return kết quả chấm (số câu đúng, tổng câu), hoặc 400 nếu payload sai định dạng
     * @throws com.datn.engflow.exception.BadRequestException nếu phiên không tồn tại/hết hạn hoặc không thuộc user
     */
    @PostMapping("/submit")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> submitGameResult(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, Object> payload) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Object sessionIdRaw = payload.get("sessionId");
        if (!(sessionIdRaw instanceof String) || ((String) sessionIdRaw).isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Session ID không được để trống"));
        }
        String sessionId = (String) sessionIdRaw;
        // New secure path: if client sends detailed answers, server validates
        Object answersObj = payload.get("answers");
        if (answersObj != null && !(answersObj instanceof java.util.List)) {
            return ResponseEntity.badRequest().body(Map.of("error", "answers phải là một danh sách"));
        }
        Object correctRawObj = payload.get("correctAnswers");
        if (correctRawObj != null && !(correctRawObj instanceof Number)) {
            return ResponseEntity.badRequest().body(Map.of("error", "correctAnswers phải là một số"));
        }
        Number correctRaw = (Number) correctRawObj;
        if (answersObj instanceof java.util.List) {
            java.util.List<?> rawAnswers = (java.util.List<?>) answersObj;
            for (Object item : rawAnswers) {
                if (!(item instanceof java.util.Map)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "mỗi phần tử answers phải là một đối tượng"));
                }
            }
            java.util.List<Map<String, Object>> answers = (java.util.List<Map<String, Object>>) answersObj;
            int clientCorrect = correctRaw != null ? correctRaw.intValue() : 0;
            return ResponseEntity.ok(gameService.submitGameResult(userPrincipal.getId(), sessionId, clientCorrect, answers));
        }
        if (correctRaw == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "correctAnswers không được để trống"));
        }
        int correctAnswers = correctRaw.intValue();

        return ResponseEntity.ok(gameService.submitGameResult(userPrincipal.getId(), sessionId, correctAnswers));
    }
}
