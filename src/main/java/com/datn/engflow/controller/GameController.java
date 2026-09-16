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

    @GetMapping("/quiz/{deckId}")
    public ResponseEntity<?> getQuizGame(@PathVariable Long deckId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gameService.generateQuiz(deckId, userPrincipal.getId()));
    }

    @GetMapping("/memory/{deckId}")
    public ResponseEntity<?> getMemoryGame(@PathVariable Long deckId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gameService.generateMemoryMatch(deckId, userPrincipal.getId()));
    }

    @GetMapping("/typing/{deckId}")
    public ResponseEntity<?> getTypingGame(@PathVariable Long deckId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gameService.generateTyping(deckId, userPrincipal.getId()));
    }

    @GetMapping("/listening/{deckId}")
    public ResponseEntity<?> getListeningGame(@PathVariable Long deckId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gameService.generateListening(deckId, userPrincipal.getId()));
    }

    @GetMapping("/mixed/{deckId}")
    public ResponseEntity<?> getMixedGame(@PathVariable Long deckId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(gameService.generateMixed(deckId, userPrincipal.getId()));
    }

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
