package com.datn.engflow.repository;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * interface ExerciseRepository.
 */
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    @Query("SELECT e FROM Exercise e JOIN FETCH e.lesson WHERE e.lesson.id = :lessonId ORDER BY e.orderIndex ASC")
    List<Exercise> findByLessonIdOrderByOrderIndexAsc(@Param("lessonId") Long lessonId);
    List<Exercise> findByLessonIdAndExerciseTypeOrderByOrderIndexAsc(Long lessonId, String exerciseType);
    List<Exercise> findByLessonIdAndDifficultyOrderByOrderIndexAsc(Long lessonId, String difficulty);
    @Query("""
            SELECT e FROM Exercise e
            JOIN FETCH e.lesson l
            WHERE (:lessonId IS NULL OR l.id = :lessonId)
              AND (:type IS NULL OR :type = '' OR e.exerciseType = :type)
              AND (:difficulty IS NULL OR :difficulty = '' OR e.difficulty = :difficulty)
              AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(e.question) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(e.explanation, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Exercise> findAdminPage(@Param("lessonId") Long lessonId,
                                 @Param("type") ExerciseType type,
                                 @Param("difficulty") ExerciseDifficulty difficulty,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

    long countByLessonId(Long lessonId);

    @Modifying
    @Transactional
    void deleteAllByLessonId(Long lessonId);

    /**
     * Returns exercises that have a non-null question and a missing or empty correctAnswer
     * (the backfill pipeline rewrites these with AI-generated keys). Returns a Page for memory
     * safety on the 43k-row exercises table.
     * JOIN FETCH lesson: the backfill walks candidates outside any transaction and reads
     * e.getLesson() (lazy) — without the fetch the very first access throws
     * LazyInitializationException (bug (a), audit plan P3.1).
     */
    @Query("SELECT e FROM Exercise e JOIN FETCH e.lesson WHERE (e.correctAnswer IS NULL OR TRIM(e.correctAnswer) = '') AND e.question IS NOT NULL AND TRIM(e.question) <> '' ORDER BY e.id ASC")
    List<Exercise> findBackfillCandidates(Pageable pageable);
}
