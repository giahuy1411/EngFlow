package com.datn.engflow.repository;

import com.datn.engflow.model.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
/**
 * interface UserStreakRepository.
 */
public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {
    Optional<UserStreak> findByUserIdAndStudyDate(Long userId, LocalDate studyDate);
    List<UserStreak> findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(Long userId, LocalDate startDate, LocalDate endDate);
}
