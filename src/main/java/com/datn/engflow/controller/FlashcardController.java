package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.FlashcardReviewRequest;
import com.datn.engflow.service.FlashcardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/flashcards")
@RequiredArgsConstructor
/**
 * class FlashcardController.
 */
public class FlashcardController {

    private final FlashcardService flashcardService;

    @PostMapping("/review")
    public ResponseEntity<?> reviewFlashcard(@Valid @RequestBody FlashcardReviewRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        String email = authentication.getName();
        flashcardService.reviewFlashcard(request, email);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/status/{vocabularyId}")
    public ResponseEntity<Integer> getStatus(@PathVariable Long vocabularyId, Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        String email = authentication.getName();
        Integer masteryLevel = flashcardService.getStatus(vocabularyId, email);
        return ResponseEntity.ok(masteryLevel);
    }

    /**
     * audit-v12 F153: the drill records its study day here so reading flashcards still counts
     * toward the streak. It no longer sends an SM-2 quality, so {@code /review} is not called
     * by the UI — this endpoint replaces it as the streak source for flashcards.
     */
    @PostMapping("/study")
    public ResponseEntity<?> recordStudyDay(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        flashcardService.recordStudyDay(authentication.getName());
        return ResponseEntity.ok().build();
    }
}
