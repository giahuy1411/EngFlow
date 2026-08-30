package com.datn.engflow.repository;

import com.datn.engflow.model.entity.VideoLesson;
import com.datn.engflow.model.enums.LessonLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * interface VideoLessonRepository.
 */
public interface VideoLessonRepository extends JpaRepository<VideoLesson, Long> {

    Page<VideoLesson> findByIsPublishedTrue(Pageable pageable);

    Page<VideoLesson> findByIsPublishedTrueAndLevel(LessonLevel level, Pageable pageable);
}
