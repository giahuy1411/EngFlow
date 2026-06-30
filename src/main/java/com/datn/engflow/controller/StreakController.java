package com.datn.engflow.controller;

import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/streak")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;

    @PostMapping("/checkin")
    public ResponseEntity<?> checkin(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, Integer> payload) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int words = payload.getOrDefault("wordsStudied", 0);
        int games = payload.getOrDefault("gamesPlayed", 0);
        if (words < 0 || games < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Giá trị không được âm"));
        }
        if (words > 1000 || games > 100) {
            return ResponseEntity.badRequest().body(Map.of("error", "Giá trị vượt quá giới hạn cho phép"));
        }
        
        return ResponseEntity.ok(streakService.checkin(userPrincipal.getId(), words, games));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getStreakHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "30") int days) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(streakService.getStreakHistory(userPrincipal.getId(), days));
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentStreak(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("currentStreak", streakService.getCurrentStreak(userPrincipal.getId())));
    }
}
