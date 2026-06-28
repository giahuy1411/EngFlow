package com.datn.engflow.repository;

import com.datn.engflow.model.entity.LessonSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonSnapshotRepository extends JpaRepository<LessonSnapshot, Long> {
    List<LessonSnapshot> findByLessonIdOrderByCreatedAtDesc(Long lessonId);
    void deleteByLessonId(Long lessonId);
}
