package com.datn.engflow.repository;

import com.datn.engflow.model.entity.ExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * interface ExerciseAttemptRepository.
 */
public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, Long> {
    List<ExerciseAttempt> findByUserIdAndLessonIdOrderByCompletedAtDesc(Long userId, Long lessonId);
    Optional<ExerciseAttempt> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Transactional
    void deleteAllByLessonId(Long lessonId);
}
