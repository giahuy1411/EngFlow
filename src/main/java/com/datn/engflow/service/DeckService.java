package com.datn.engflow.service;

import com.datn.engflow.model.entity.Deck;
import com.datn.engflow.model.dto.DeckRequest;

import java.util.List;

public interface DeckService {
    List<Deck> getAllPublicDecks();
    List<Deck> getUserDecks(Long userId);
    Deck getDeckById(Long deckId, Long userId);
    Deck createDeck(DeckRequest request, Long userId);
    Deck updateDeck(Long deckId, DeckRequest request, Long userId);
    void deleteDeck(Long deckId, Long userId);
    void addWordToDeck(Long deckId, Long vocabId, Long userId);
}
