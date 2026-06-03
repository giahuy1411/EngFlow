package com.datn.engflow.controller;

import com.datn.engflow.service.AiVocabService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiVocabController {

    private final AiVocabService aiVocabService;

    @PostMapping("/generate-vocab")
    public ResponseEntity<?> generateVocab(@RequestBody Map<String, String> payload) {
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
        
        return ResponseEntity.ok(aiVocabService.generateVocabByTopic(topic, level, count).block());
    }

    @PostMapping("/enrich-word")
    public ResponseEntity<?> enrichWord(@RequestBody Map<String, String> payload) {
        String word = payload.get("word");
        if (word == null || word.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "word không được để trống"));
        }
        return ResponseEntity.ok(aiVocabService.enrichWord(word).block());
    }
}
