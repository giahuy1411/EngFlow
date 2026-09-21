package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.entity.Deck;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.UserVocabularyProgressRepository;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * audit-v12 F151: {@code GET /api/srs/due/{deckId}} used to read deck_words for ANY deckId
 * without checking who owns the deck. Measured live: a student received 200 and the word
 * content of an admin's PRIVATE deck (is_public=0) — the very same deck that
 * {@code GET /api/decks/{id}} correctly refuses with 400.
 *
 * <p>The fix delegates to {@link DeckService#getDeckById} so the rule lives in one place.
 * These tests pin the three outcomes.
 */
@ExtendWith(MockitoExtension.class)
class SrsDueWordsAuthzTest {
    @Mock private UserVocabularyProgressRepository progressRepository;
    @Mock private UserRepository userRepository;
    @Mock private VocabularyRepository vocabularyRepository;
    @Mock private DeckWordRepository deckWordRepository;
    @Mock private StudyActivityService studyActivityService;
    @Mock private DeckService deckService;

    private SrsService srsService;

    @BeforeEach
    void setUp() {
        srsService = new SrsService(progressRepository, userRepository, vocabularyRepository,
                deckWordRepository, studyActivityService, deckService);
    }

    /** The regression: a private deck belonging to somebody else must be refused. */
    @Test
    void privateDeckOfAnotherUserIsRefused() {
        doThrow(new BadRequestException("Bạn không có quyền truy cập bộ từ vựng này"))
                .when(deckService).getDeckById(30033L, 2L);

        assertThatThrownBy(() -> srsService.getDueWords(2L, 30033L))
                .isInstanceOf(BadRequestException.class);

        // Must fail BEFORE touching deck_words — no partial read.
        verifyNoInteractions(deckWordRepository);
    }

    /** The owner still reads their own private deck. */
    @Test
    void ownPrivateDeckIsAllowed() {
        when(deckService.getDeckById(50038L, 2L))
                .thenReturn(Deck.builder().id(50038L).isPublic(false).owner(User.builder().id(2L).build()).build());
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(50038L)).thenReturn(List.of());

        assertThat(srsService.getDueWords(2L, 50038L)).isEmpty();
    }

    /** Public decks stay readable by everyone — the fix must not lock those down. */
    @Test
    void publicDeckIsAllowed() {
        when(deckService.getDeckById(10006L, 2L))
                .thenReturn(Deck.builder().id(10006L).isPublic(true).build());
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(10006L)).thenReturn(List.of());

        assertThat(srsService.getDueWords(2L, 10006L)).isEmpty();
    }

    /** A deck that does not exist is a 404, not an empty 200. */
    @Test
    void missingDeckIsNotFound() {
        doThrow(new ResourceNotFoundException("Deck", "id", 999999L))
                .when(deckService).getDeckById(999999L, 2L);

        assertThatThrownBy(() -> srsService.getDueWords(2L, 999999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
