package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.DeckRequest;
import com.datn.engflow.model.dto.response.DeckSummaryResponse;
import com.datn.engflow.model.entity.Deck;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.DeckRepository;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * class DeckService.
 */
public class DeckService {

    private final DeckRepository deckRepository;
    private final DeckWordRepository deckWordRepository;
    private final UserRepository userRepository;
    private final VocabularyRepository vocabularyRepository;

    public List<Deck> getAllPublicDecks() {
        return getAllPublicDecks(null);
    }

    public List<Deck> getAllPublicDecks(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return deckRepository.searchPublic(keyword.trim());
        }
        return deckRepository.findByIsPublicTrue();
    }

    public List<Deck> getUserDecks(Long userId) {
        return getUserDecks(userId, null);
    }

    public List<Deck> getUserDecks(Long userId, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return deckRepository.searchByOwner(userId, keyword.trim());
        }
        return deckRepository.findByOwnerId(userId);
    }

    public Page<DeckSummaryResponse> getPublicDeckPage(String keyword, Pageable pageable) {
        Page<Deck> page = deckRepository.findPublicPage(normalizeKeyword(keyword), pageable);
        Page<DeckSummaryResponse> summary = page.map(this::toSummary);
        attachWordCounts(summary);
        return summary;
    }

    public Page<DeckSummaryResponse> getUserDeckPage(Long userId, String keyword, Pageable pageable) {
        Page<Deck> page = deckRepository.findOwnerPage(userId, normalizeKeyword(keyword), pageable);
        Page<DeckSummaryResponse> summary = page.map(this::toSummary);
        attachWordCounts(summary);
        return summary;
    }

    private String normalizeKeyword(String keyword) {
        return keyword != null && !keyword.isBlank() ? keyword.trim() : null;
    }

    private DeckSummaryResponse toSummary(Deck deck) {
        return DeckSummaryResponse.builder()
                .id(deck.getId())
                .name(deck.getName())
                .description(deck.getDescription())
                .source(deck.getSource())
                .cefrLevel(deck.getCefrLevel())
                .isPublic(deck.getIsPublic())
                .thumbnailUrl(deck.getThumbnailUrl())
                .wordCount(null)
                .createdAt(deck.getCreatedAt())
                .updatedAt(deck.getUpdatedAt())
                .build();
    }

    private void attachWordCounts(Page<DeckSummaryResponse> page) {
        List<Long> deckIds = page.getContent().stream().map(DeckSummaryResponse::getId).toList();
        if (deckIds.isEmpty()) return;
        Map<Long, Long> counts = deckRepository.countWordsByDeckIds(deckIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        page.getContent().forEach(dto -> dto.setWordCount(counts.getOrDefault(dto.getId(), 0L).intValue()));
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
