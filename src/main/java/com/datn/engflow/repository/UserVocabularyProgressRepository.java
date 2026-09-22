package com.datn.engflow.repository;

import com.datn.engflow.model.entity.UserVocabularyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * interface UserVocabularyProgressRepository.
 */
public interface UserVocabularyProgressRepository extends JpaRepository<UserVocabularyProgress, Long> {
    Optional<UserVocabularyProgress> findByUserIdAndVocabularyId(Long userId, Long vocabularyId);

    List<UserVocabularyProgress> findByUserId(Long userId);

    /**
     * audit-v13 F-13-15: batch lookup so the due-words path can fetch all progress rows
     * for a deck in ONE query instead of one query per word (measured N+1: the due-words
     * endpoint issued a query per deck word — proven by query-stats delta, not by reading
     * the code).
     */
    List<UserVocabularyProgress> findByUserIdAndVocabularyIdIn(Long userId, java.util.Collection<Long> vocabularyIds);
}
