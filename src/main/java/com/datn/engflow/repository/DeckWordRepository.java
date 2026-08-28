package com.datn.engflow.repository;

import com.datn.engflow.model.entity.DeckWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * interface DeckWordRepository.
 */
public interface DeckWordRepository extends JpaRepository<DeckWord, Long> {
    @Query("SELECT dw FROM DeckWord dw JOIN FETCH dw.vocabulary WHERE dw.deck.id = :deckId ORDER BY dw.orderIndex ASC")
    List<DeckWord> findByDeckIdOrderByOrderIndexAsc(@Param("deckId") Long deckId);
    Optional<DeckWord> findByDeckIdAndVocabularyId(Long deckId, Long vocabId);
    boolean existsByDeckIdAndVocabularyId(Long deckId, Long vocabId);
}
