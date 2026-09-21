package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * audit-v12 F147: {@code vocabulary} is a shared dictionary with no owner column; ownership
 * of a saved word lives in {@code decks.owner_id} + {@code deck_words}. These tests lock the
 * three rules that keep a learner's save from polluting the shared dictionary.
 */
@ExtendWith(MockitoExtension.class)
class VocabularyServiceTest {
    @Mock private VocabularyRepository vocabularyRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private DeckService deckService;
    @InjectMocks private VocabularyService service;

    private VocabularyRequest request(String word) {
        VocabularyRequest r = new VocabularyRequest();
        r.setWord(word);
        r.setMeaning("nghĩa");
        return r;
    }

    /** A non-admin must name a deck; without one the write would hit the shared dictionary. */
    @Test
    void nonAdminWithoutDeckIsRejected() {
        assertThatThrownBy(() -> service.createScoped(request("hello"), null, 7L, false))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(vocabularyRepository, deckService);
    }

    /** With a deck, the word is saved AND linked in one transaction (via the owning service). */
    @Test
    void nonAdminWithDeckSavesAndLinks() {
        when(vocabularyRepository.findByWordContainingIgnoreCase("hello")).thenReturn(List.of());
        when(vocabularyRepository.save(any())).thenAnswer(i -> {
            Vocabulary v = i.getArgument(0);
            v.setId(500L);
            return v;
        });

        Vocabulary saved = service.createScoped(request("hello"), 10006L, 7L, false);

        assertThat(saved.getId()).isEqualTo(500L);
        // The link goes through DeckService, which owns the ownership check + idempotency.
        verify(deckService).addWordToDeck(10006L, 500L, 7L);
    }

    /** Re-saving a word must reuse the existing dictionary row, not add a duplicate. */
    @Test
    void reusesExistingWordInsteadOfDuplicating() {
        Vocabulary existing = Vocabulary.builder().id(10017L).word("negotiate").build();
        when(vocabularyRepository.findByWordContainingIgnoreCase("negotiate")).thenReturn(List.of(existing));

        Vocabulary saved = service.createScoped(request("negotiate"), 10006L, 7L, false);

        assertThat(saved.getId()).isEqualTo(10017L);
        verify(vocabularyRepository, never()).save(any());
        verify(deckService).addWordToDeck(10006L, 10017L, 7L);
    }

    /** A lesson-scoped word is curriculum content and must not be borrowed by a deck. */
    @Test
    void doesNotReuseLessonScopedWord() {
        Vocabulary lessonWord = Vocabulary.builder().id(1L).word("analyze")
                .lesson(com.datn.engflow.model.entity.Lesson.builder().id(9L).build()).build();
        when(vocabularyRepository.findByWordContainingIgnoreCase("analyze")).thenReturn(List.of(lessonWord));
        when(vocabularyRepository.save(any())).thenAnswer(i -> {
            Vocabulary v = i.getArgument(0);
            v.setId(777L);
            return v;
        });

        Vocabulary saved = service.createScoped(request("analyze"), 10006L, 7L, false);

        assertThat(saved.getId()).isEqualTo(777L);
    }

    /** Admins curate the shared dictionary and may do so without a deck. */
    @Test
    void adminMayCreateWithoutDeck() {
        when(vocabularyRepository.findByWordContainingIgnoreCase("curated")).thenReturn(List.of());
        when(vocabularyRepository.save(any())).thenAnswer(i -> {
            Vocabulary v = i.getArgument(0);
            v.setId(900L);
            return v;
        });

        Vocabulary saved = service.createScoped(request("curated"), null, 1L, true);

        assertThat(saved.getId()).isEqualTo(900L);
        verify(deckService, never()).addWordToDeck(any(), any(), any());
    }

    /** lessonId used to be silently dropped by the public endpoint. */
    @Test
    void honoursLessonIdWhenProvided() {
        when(lessonRepository.findById(42L)).thenReturn(java.util.Optional.of(
                com.datn.engflow.model.entity.Lesson.builder().id(42L).build()));
        when(vocabularyRepository.findByWordContainingIgnoreCase("word")).thenReturn(List.of());
        when(vocabularyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VocabularyRequest r = request("word");
        r.setLessonId(42L);
        service.createScoped(r, null, 1L, true);

        ArgumentCaptor<Vocabulary> captor = ArgumentCaptor.forClass(Vocabulary.class);
        verify(vocabularyRepository).save(captor.capture());
        assertThat(captor.getValue().getLesson()).isNotNull();
        assertThat(captor.getValue().getLesson().getId()).isEqualTo(42L);
    }
}
