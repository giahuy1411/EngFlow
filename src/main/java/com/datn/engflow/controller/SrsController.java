package com.datn.engflow.controller;

import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.SrsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/srs")
@RequiredArgsConstructor
public class SrsController {

    private final SrsService srsService;

    @PostMapping("/review")
    public ResponseEntity<?> reviewWord(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, Integer> payload) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Integer vocabIdObj = payload.get("vocabId");
        Integer qualityObj = payload.get("quality");
        if (vocabIdObj == null || qualityObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "vocabId và quality không được để trống"));
        }
        Long vocabId = vocabIdObj.longValue();
        int quality = qualityObj;
        if (quality < 0 || quality > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "quality phải từ 0 đến 5"));
        }
        srsService.reviewWord(userPrincipal.getId(), vocabId, quality);
        return ResponseEntity.ok(Map.of("message", "Review recorded successfully"));
    }

    @GetMapping("/due/{deckId}")
    public ResponseEntity<?> getDueWords(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long deckId) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(srsService.getDueWords(userPrincipal.getId(), deckId));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStudyStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(srsService.getStudyStats(userPrincipal.getId()));
    }
}
