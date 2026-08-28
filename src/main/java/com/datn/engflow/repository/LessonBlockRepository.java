package com.datn.engflow.repository;

import com.datn.engflow.model.entity.LessonBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * interface LessonBlockRepository.
 */
public interface LessonBlockRepository extends JpaRepository<LessonBlock, Long> {
    List<LessonBlock> findBySectionIdOrderByOrderIndexAsc(Long sectionId);
    void deleteBySectionId(Long sectionId);
}
