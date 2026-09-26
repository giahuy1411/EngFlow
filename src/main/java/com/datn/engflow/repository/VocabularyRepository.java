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
    List<Vocabulary> findByWordContainingIgnoreCase(String word);

    /**
     * audit-v17 F-17-05: exact (case-insensitive) word match, for the local fast path.
     *
     * <p>The dictionary lookup can take ~20 s when the upstream is cold (measured 20.7 s), and
     * the failure path is not cached. When the word is already in the local vocabulary table we
     * can answer in single-digit milliseconds instead of waiting on the network. This is an
     * exact match — {@link #findByWordContainingIgnoreCase} is a leading-wildcard LIKE, which
     * would return the wrong rows to "settle" a lookup.
     */
    List<Vocabulary> findByWordIgnoreCase(String word);

    void deleteByLessonId(Long lessonId);
}
