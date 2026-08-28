package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.DeckSummaryResponse;
import com.datn.engflow.model.entity.Deck;
import com.datn.engflow.repository.DeckRepository;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeckServicePaginationTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private DeckWordRepository deckWordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @InjectMocks
    private DeckService deckService;

    private Deck deck(Long id, String name, boolean isPublic) {
        return Deck.builder()
                .id(id)
                .name(name)
                .description("desc " + id)
                .source("oxford3000")
                .cefrLevel("B1")
                .isPublic(isPublic)
                .build();
    }

    @Test
    void publicDeckPageReturnsSummaryWithoutOwnerOrWords() {
        Pageable pageable = PageRequest.of(0, 9);
        Deck deck = deck(100L, "Oxford 3000", true);
        Page<Deck> page = new PageImpl<>(List.of(deck), pageable, 1);
        when(deckRepository.findPublicPage(null, pageable)).thenReturn(page);
        when(deckRepository.countWordsByDeckIds(List.of(100L)))
                .thenReturn(List.<Object[]>of(new Object[]{100L, 42L}));

        Page<DeckSummaryResponse> result = deckService.getPublicDeckPage(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        DeckSummaryResponse dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getName()).isEqualTo("Oxford 3000");
        assertThat(dto.getWordCount()).isEqualTo(42);
        // Summary DTO must not leak owner or word entities
        assertThat(dto.getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("owner", "words");
        verify(deckRepository).findPublicPage(null, pageable);
        verify(deckRepository).countWordsByDeckIds(List.of(100L));
    }

    @Test
    void userDeckPageOnlyContainsOwnersDecks() {
        Pageable pageable = PageRequest.of(0, 9);
        Deck deck = deck(200L, "My Deck", false);
        Page<Deck> page = new PageImpl<>(List.of(deck), pageable, 1);
        when(deckRepository.findOwnerPage(7L, null, pageable)).thenReturn(page);
        when(deckRepository.countWordsByDeckIds(List.of(200L)))
                .thenReturn(List.<Object[]>of(new Object[]{200L, 5L}));

        Page<DeckSummaryResponse> result = deckService.getUserDeckPage(7L, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(200L);
        assertThat(result.getContent().get(0).getIsPublic()).isFalse();
        assertThat(result.getContent().get(0).getWordCount()).isEqualTo(5);
        verify(deckRepository).findOwnerPage(7L, null, pageable);
    }

    @Test
    void keywordIsTrimmedBeforeQuery() {
        Pageable pageable = PageRequest.of(0, 9);
        when(deckRepository.findPublicPage("travel", pageable))
                .thenReturn(Page.empty(pageable));

        Page<DeckSummaryResponse> result = deckService.getPublicDeckPage("  travel  ", pageable);

        assertThat(result.getContent()).isEmpty();
        verify(deckRepository).findPublicPage("travel", pageable);
    }
}
