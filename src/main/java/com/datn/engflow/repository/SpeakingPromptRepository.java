package com.datn.engflow.repository;

import com.datn.engflow.model.entity.SpeakingPrompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * interface SpeakingPromptRepository.
 */
public interface SpeakingPromptRepository extends JpaRepository<SpeakingPrompt, Long> {
    Page<SpeakingPrompt> findByIsPublishedTrue(Pageable pageable);
    List<SpeakingPrompt> findByIsPublishedTrueOrderByOrderIndexAsc();
    List<SpeakingPrompt> findByIsPremiumFalseAndIsPublishedTrueOrderByOrderIndexAsc();
    List<SpeakingPrompt> findAllByOrderByOrderIndexAsc();

    @Query("SELECT sp FROM SpeakingPrompt sp WHERE LOWER(COALESCE(sp.title, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(sp.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(sp.level, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(sp.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<SpeakingPrompt> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    void deleteByLessonId(Long lessonId);
}