package com.datn.engflow.repository;

import com.datn.engflow.model.entity.SpeakingSubmissionStatus;
import com.datn.engflow.model.entity.SpeakingSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * interface SpeakingSubmissionRepository.
 */
public interface SpeakingSubmissionRepository extends JpaRepository<SpeakingSubmission, Long> {
    Page<SpeakingSubmission> findByUserId(Long userId, Pageable pageable);
    Page<SpeakingSubmission> findByUserIdAndPromptId(Long userId, Long promptId, Pageable pageable);
    List<SpeakingSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId);
    List<SpeakingSubmission> findByPromptIdOrderBySubmittedAtDesc(Long promptId);
    List<SpeakingSubmission> findAllByOrderBySubmittedAtDesc();
    Page<SpeakingSubmission> findByStatus(SpeakingSubmissionStatus status, Pageable pageable);
    boolean existsByPromptId(Long promptId);
    // audit-v7 F55: media-proxy legacy-URL ownership fallback
    boolean existsByMediaObjectKeyAndUserId(String mediaObjectKey, Long userId);
}
