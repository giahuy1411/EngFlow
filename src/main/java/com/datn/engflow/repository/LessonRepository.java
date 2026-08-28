package com.datn.engflow.repository;

import com.datn.engflow.model.dto.projection.LessonListProjection;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.LessonLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * interface LessonRepository.
 */
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByIsPublishedTrueOrderByOrderIndexAsc();
    List<Lesson> findByLevelAndIsPublishedTrue(LessonLevel level);
    long countByIsPublishedTrue();
    Optional<Lesson> findByTitle(String title);
    List<Lesson> findByTitleContainingIgnoreCase(String keyword);

    @Query("SELECT l FROM Lesson l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(l.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Lesson> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT l FROM Lesson l WHERE LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) "
           + "OR LOWER(COALESCE(l.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Lesson> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT l FROM Lesson l " +
           "LEFT JOIN FETCH l.vocabularies " +
           "WHERE l.id = :id")
    Optional<Lesson> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT l FROM Lesson l
            WHERE l.isPublished = true
              AND (:level IS NULL OR l.level = :level)
              AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Lesson> findPublishedPage(@Param("keyword") String keyword, @Param("level") LessonLevel level, Pageable pageable);

    /**
     * Lightweight list query — selects only the columns the lesson list renders,
     * skipping the NVARCHAR(MAX) content columns (huge read savings on this table).
     */
    @Query("""
            SELECT l.id AS id, l.title AS title, l.description AS description,
                   l.level AS level, l.category AS category,
                   l.durationMinutes AS durationMinutes, l.thumbnailUrl AS thumbnailUrl,
                   l.audioUrl AS audioUrl, l.skillType AS skillType, l.orderIndex AS orderIndex
            FROM Lesson l
            WHERE l.isPublished = true
              AND (:level IS NULL OR l.level = :level)
              AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<LessonListProjection> findPublishedPageProjection(@Param("keyword") String keyword,
                                                           @Param("level") LessonLevel level,
                                                           Pageable pageable);

    @Query("""
            SELECT l FROM Lesson l
            WHERE (:level IS NULL OR l.level = :level)
              AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(l.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Lesson> findAdminPage(@Param("keyword") String keyword, @Param("level") LessonLevel level, Pageable pageable);
}
