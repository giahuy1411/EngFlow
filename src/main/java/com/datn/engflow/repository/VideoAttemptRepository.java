package com.datn.engflow.repository;

import com.datn.engflow.model.entity.VideoAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * interface VideoAttemptRepository.
 */
public interface VideoAttemptRepository extends JpaRepository<VideoAttempt, Long> {

    Page<VideoAttempt> findByUserId(Long userId, Pageable pageable);

    Page<VideoAttempt> findByStatus(String status, Pageable pageable);

    // audit-v7 F55: media-proxy legacy-URL ownership fallback
    boolean existsByMediaObjectKeyAndUserId(String mediaObjectKey, Long userId);

    @Query("select a.lineIndex from VideoAttempt a where a.user.id = :userId and a.videoLesson.id = :lessonId")
    List<Integer> findCompletedLineIndexes(@Param("userId") Long userId, @Param("lessonId") Long lessonId);
}
