package com.datn.engflow.repository;

import com.datn.engflow.model.entity.ExerciseSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseSubmissionRepository extends JpaRepository<ExerciseSubmission, Long> {
    List<ExerciseSubmission> findByUserId(Long userId);
    Optional<ExerciseSubmission> findByUserIdAndExerciseId(Long userId, Long exerciseId);

    @Query("SELECT es.exercise.id FROM ExerciseSubmission es WHERE es.user.id = :userId AND es.exercise.lesson.id = :lessonId AND es.isCorrect = true")
    List<Long> findCorrectExerciseIdsByUserIdAndLessonId(@Param("userId") Long userId, @Param("lessonId") Long lessonId);

    @Query("SELECT COUNT(es) FROM ExerciseSubmission es WHERE es.user.id = :userId AND es.isCorrect = true")
    Long countCorrectByUserId(@Param("userId") Long userId);

    @Query("SELECT es FROM ExerciseSubmission es LEFT JOIN FETCH es.exercise WHERE es.user.id = :userId AND es.submittedAt >= :since AND es.isCorrect = true")
    List<ExerciseSubmission> findCorrectByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT es FROM ExerciseSubmission es LEFT JOIN FETCH es.exercise WHERE es.user.id = :userId AND es.submittedAt >= :start AND es.submittedAt <= :end AND es.isCorrect = true")
    List<ExerciseSubmission> findCorrectByUserIdBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
