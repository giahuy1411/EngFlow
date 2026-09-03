package com.datn.engflow.repository;

import com.datn.engflow.model.entity.LessonBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
/**
 * interface LessonBlockRepository.
 */
public interface LessonBlockRepository extends JpaRepository<LessonBlock, Long> {
    List<LessonBlock> findBySectionIdOrderByOrderIndexAsc(Long sectionId);
    void deleteBySectionId(Long sectionId);

    /**
     * audit-v5 perf: snapshot/restore từng query blocks theo từng section (N+1).
     * Một query cho cả lesson, group by sectionId phía service.
     */
    @Query("select b from LessonBlock b where b.section.id in :sectionIds order by b.section.id, b.orderIndex")
    List<LessonBlock> findBySectionIds(@Param("sectionIds") Collection<Long> sectionIds);
}
