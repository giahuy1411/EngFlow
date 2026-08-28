package com.datn.engflow.repository;

import com.datn.engflow.model.entity.LessonSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * interface LessonSectionRepository.
 */
public interface LessonSectionRepository extends JpaRepository<LessonSection, Long> {
    List<LessonSection> findByLessonIdOrderByOrderIndexAsc(Long lessonId);
    void deleteByLessonId(Long lessonId);
}
