package com.datn.engflow.repository;

import com.datn.engflow.model.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
/**
 * interface ProgressRepository.
 */
public interface ProgressRepository extends JpaRepository<Progress, Long> {
    Optional<Progress> findByUserIdAndLessonId(Long userId, Long lessonId);
    List<Progress> findByUserId(Long userId);
    List<Progress> findByUserIdAndLessonIdIn(Long userId, Collection<Long> lessonIds);
    Long countByUserIdAndIsCompletedTrue(Long userId);
    void deleteByLessonId(Long lessonId);
}
