package com.datn.engflow.repository;

import com.datn.engflow.model.entity.LessonSubmission;
import com.datn.engflow.model.enums.SkillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/**
 * interface LessonSubmissionRepository.
 */
public interface LessonSubmissionRepository extends JpaRepository<LessonSubmission, Long> {
    Optional<LessonSubmission> findByUserIdAndLessonIdAndSkillType(Long userId, Long lessonId, SkillType skillType);
    void deleteByLessonId(Long lessonId);
}
