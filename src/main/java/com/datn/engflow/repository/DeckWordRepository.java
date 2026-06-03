package com.datn.engflow.repository;

import com.datn.engflow.model.entity.DeckWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeckWordRepository extends JpaRepository<DeckWord, Long> {
    List<DeckWord> findByDeckIdOrderByOrderIndexAsc(Long deckId);
    Optional<DeckWord> findByDeckIdAndVocabularyId(Long deckId, Long vocabId);
    boolean existsByDeckIdAndVocabularyId(Long deckId, Long vocabId);
}
