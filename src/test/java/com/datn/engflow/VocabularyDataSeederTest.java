package com.datn.engflow;

import com.datn.engflow.model.entity.Deck;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.DeckRepository;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VocabularyDataSeederTest {

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private DeckWordRepository deckWordRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Test
    void shouldHaveAtLeastTenDecks() {
        List<Deck> seededDecks = deckRepository.findAll().stream()
                .filter(deck -> deck.getOwner() == null)
                .toList();
        assertTrue(seededDecks.size() >= 10,
                "Should have at least 10 seeded decks, but found " + seededDecks.size());
    }

    @Test
    void eachDeckShouldHaveAtLeastTenWords() {
        List<Deck> seededDecks = deckRepository.findAll().stream()
                .filter(deck -> deck.getOwner() == null)
                .toList();
        assertFalse(seededDecks.isEmpty(), "No seeded decks found in database");
        for (Deck deck : seededDecks) {
            List<DeckWord> words = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deck.getId());
            assertTrue(words.size() >= 10,
                    "Deck '" + deck.getName() + "' should have at least 10 words, but has " + words.size());
        }
    }

    @Test
    void eachDeckWordShouldHaveCompleteData() {
        List<Deck> seededDecks = deckRepository.findAll().stream()
                .filter(deck -> deck.getOwner() == null)
                .toList();
        assertFalse(seededDecks.isEmpty(), "No seeded decks found in database");
        for (Deck deck : seededDecks) {
            List<DeckWord> deckWords = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deck.getId());
            for (DeckWord dw : deckWords) {
                Vocabulary v = dw.getVocabulary();
                assertNotNull(v, "Vocabulary should not be null in deck " + deck.getName());
                assertNotNull(v.getWord(), "Word should not be null");
                assertNotNull(v.getPronunciation(), "Pronunciation missing for: " + v.getWord());
                assertNotNull(v.getMeaning(), "Vietnamese meaning missing for: " + v.getWord());
                assertNotNull(v.getDefinitionEn(), "English definition missing for: " + v.getWord());
                assertNotNull(v.getExampleSentence(), "Example sentence missing for: " + v.getWord());
                assertNotNull(v.getWordType(), "Word type missing for: " + v.getWord());
                assertNotNull(v.getCefrLevel(), "CEFR level missing for: " + v.getWord());
            }
        }
    }

    @Test
    void shouldHaveAtLeastFourDistinctSources() {
        List<Deck> seededDecks = deckRepository.findAll().stream()
                .filter(deck -> deck.getOwner() == null)
                .toList();
        long uniqueSources = seededDecks.stream()
                .map(Deck::getSource)
                .distinct()
                .count();
        assertTrue(uniqueSources >= 4,
                "Should have at least 4 different deck sources, but found " + uniqueSources);
    }
}
