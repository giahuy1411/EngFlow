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
    void deleteByLessonId(Long lessonId);
}
