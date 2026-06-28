package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.DeckRequest;
import com.datn.engflow.model.entity.Deck;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.DeckRepository;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;
    private final DeckWordRepository deckWordRepository;
    private final UserRepository userRepository;
    private final VocabularyRepository vocabularyRepository;

    public List<Deck> getAllPublicDecks() {
        return deckRepository.findByIsPublicTrue();
    }

    public List<Deck> getUserDecks(Long userId) {
        return deckRepository.findByOwnerId(userId);
    }

    public Deck getDeckById(Long deckId, Long userId) {
        Deck deck = deckRepository.findById(deckId).orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
        if (!deck.getIsPublic() && (userId == null || !userId.equals(deck.getOwner().getId()))) {
            throw new BadRequestException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n truy c\u1eadp b\u1ed9 t\u1eeb v\u1ef1ng n\u00e0y");
        }
        return deck;
    }

    @Transactional
    public Deck createDeck(DeckRequest request, Long userId) {
        User owner = userRepository.findById(userId).orElseThrow();
        Deck deck = Deck.builder()
                .owner(owner)
                .name(request.getName())
                .description(request.getDescription())
                .source(request.getSource())
                .cefrLevel(request.getCefrLevel())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .thumbnailUrl(request.getThumbnailUrl())
                .build();
        return deckRepository.save(deck);
    }

    @Transactional
    public Deck updateDeck(Long deckId, DeckRequest request, Long userId) {
        Deck deck = deckRepository.findById(deckId).orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
        if (deck.getOwner() == null || !deck.getOwner().getId().equals(userId)) {
            throw new BadRequestException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n s\u1eeda b\u1ed9 t\u1eeb n\u00e0y");
        }
        deck.setName(request.getName());
        deck.setDescription(request.getDescription());
        deck.setSource(request.getSource());
        deck.setCefrLevel(request.getCefrLevel());
        if (request.getIsPublic() != null) deck.setIsPublic(request.getIsPublic());
        deck.setThumbnailUrl(request.getThumbnailUrl());
        return deckRepository.save(deck);
    }

    @Transactional
    public void deleteDeck(Long deckId, Long userId) {
        Deck deck = deckRepository.findById(deckId).orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
        if (deck.getOwner() == null || !deck.getOwner().getId().equals(userId)) {
            throw new BadRequestException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n x\u00f3a b\u1ed9 t\u1eeb n\u00e0y");
        }
        deckRepository.delete(deck);
    }

    @Transactional
    public void addWordToDeck(Long deckId, Long vocabId, Long userId) {
        Deck deck = deckRepository.findById(deckId).orElseThrow(() -> new ResourceNotFoundException("Deck", "id", deckId));
        if (deck.getOwner() == null || !deck.getOwner().getId().equals(userId)) {
            throw new BadRequestException("B\u1ea1n kh\u00f4ng c\u00f3 quy\u1ec1n ch\u1ec9nh s\u1eeda b\u1ed9 t\u1eeb n\u00e0y");
        }
        Vocabulary vocab = vocabularyRepository.findById(vocabId)
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary", "id", vocabId));

        if (deckWordRepository.existsByDeckIdAndVocabularyId(deckId, vocabId)) {
            return;
        }

        List<DeckWord> existingWords = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deckId);
        int nextOrder = existingWords.isEmpty() ? 1 : existingWords.get(existingWords.size() - 1).getOrderIndex() + 1;

        DeckWord deckWord = DeckWord.builder()
                .deck(deck)
                .vocabulary(vocab)
                .orderIndex(nextOrder)
                .build();

        deckWordRepository.save(deckWord);
    }
}
