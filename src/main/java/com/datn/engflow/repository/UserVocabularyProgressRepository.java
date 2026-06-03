package com.datn.engflow.repository;

import com.datn.engflow.model.entity.UserVocabularyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVocabularyProgressRepository extends JpaRepository<UserVocabularyProgress, Long> {
    Optional<UserVocabularyProgress> findByUserIdAndVocabularyId(Long userId, Long vocabularyId);

    List<UserVocabularyProgress> findByUserId(Long userId);
}
