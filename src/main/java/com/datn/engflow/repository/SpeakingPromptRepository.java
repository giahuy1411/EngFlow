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

    Page<SpeakingPrompt> findByIsPremiumFalseAndIsPublishedTrue(Pageable pageable);

    @Query("SELECT sp FROM SpeakingPrompt sp WHERE LOWER(COALESCE(sp.title, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(sp.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(sp.level, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(sp.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<SpeakingPrompt> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Tìm kiếm công khai có ẩn đề premium.{@code isPremium = false} được so sánh
     * tường minh thay vì {@code IS NOT TRUE} để không loại các dòng có cột NULL.
     *
     * @param keyword      từ khóa thô, chưa lowercase
     * @param hidePremium  ẩn đề premium khi true
     * @param pageable     phân trang
     * @return trang kết quả
     */
    @Query("SELECT sp FROM SpeakingPrompt sp WHERE (LOWER(COALESCE(sp.title, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(sp.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(sp.level, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(sp.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
           + "AND (:hidePremium = false OR sp.isPremium = false)")
    Page<SpeakingPrompt> searchByKeyword(@Param("keyword") String keyword,
                                         @Param("hidePremium") boolean hidePremium,
                                         Pageable pageable);

    void deleteByLessonId(Long lessonId);
}