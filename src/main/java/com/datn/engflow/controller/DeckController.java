package com.datn.engflow.controller;

import com.datn.engflow.model.dto.DeckRequest;
import com.datn.engflow.model.entity.Deck;
import com.datn.engflow.security.UserPrincipal;
import jakarta.validation.Valid;
import com.datn.engflow.service.DeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;

    @GetMapping
    public ResponseEntity<?> getAllPublicDecks() {
        return ResponseEntity.ok(deckService.getAllPublicDecks());
    }

    @GetMapping("/my")
    public ResponseEntity<?> getUserDecks(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(deckService.getUserDecks(userPrincipal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDeckById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        return ResponseEntity.ok(deckService.getDeckById(id, userId));
    }

    @PostMapping
    public ResponseEntity<?> createDeck(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody DeckRequest request) {
        return ResponseEntity.ok(deckService.createDeck(request, userPrincipal.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDeck(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody DeckRequest request) {
        return ResponseEntity.ok(deckService.updateDeck(id, request, userPrincipal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDeck(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        deckService.deleteDeck(id, userPrincipal.getId());
        return ResponseEntity.ok(Map.of("message", "Deck deleted successfully"));
    }

    @PostMapping("/{id}/words")
    public ResponseEntity<?> addWordToDeck(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody Map<String, Long> payload) {
        deckService.addWordToDeck(id, payload.get("vocabId"), userPrincipal.getId());
        return ResponseEntity.ok(Map.of("message", "Word added to deck successfully"));
    }
}
