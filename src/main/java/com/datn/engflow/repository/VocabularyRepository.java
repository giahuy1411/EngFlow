package com.datn.engflow.repository;

import com.datn.engflow.model.entity.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * interface VocabularyRepository.
 */
public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    List<Vocabulary> findByLessonId(Long lessonId);

    /**
     * Case-insensitive substring match backing the public {@code /api/vocabulary/search} endpoint.
     *
     * <p>audit-v17 closing round: the exact-match sibling ({@code findByWordIgnoreCase}) was
     * removed with the local lookup fallback. The dictionary is now the only tra-từ source; this
     * table stays the DECK store (deck_words / user_vocabulary_progress) and is not on the
     * lookup path any more.
     */
    List<Vocabulary> findByWordContainingIgnoreCase(String word);

    void deleteByLessonId(Long lessonId);
}
