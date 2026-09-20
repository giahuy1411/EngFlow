package com.datn.engflow.repository;

import com.datn.engflow.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
/**
 * interface UserRepository.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findForStudyUpdate(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND "
            + "(SELECT MAX(d.studyDate) FROM StudyDay d WHERE d.userId = u.id "
            + "AND d.studyDate BETWEEN :start AND :today) = :yesterday")
    List<User> findStudyReminderAtRisk(@Param("start") LocalDate start,
            @Param("today") LocalDate today, @Param("yesterday") LocalDate yesterday);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND "
            + "(SELECT MAX(d.studyDate) FROM StudyDay d WHERE d.userId = u.id "
            + "AND d.studyDate BETWEEN :start AND :today) < :yesterday")
    List<User> findStudyReminderBroken(@Param("start") LocalDate start,
            @Param("today") LocalDate today, @Param("yesterday") LocalDate yesterday);

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchByKeywordPage(@Param("keyword") String keyword, Pageable pageable);

    long countByIsActiveTrue();

    long countByLastStudyDateAfter(LocalDate threshold);

    /** User active đã bỏ học từ trước threshold (kể cả never-studied) — legacy, giữ cho tương thích. */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND (u.lastStudyDate IS NULL OR u.lastStudyDate < :threshold)")
    List<User> findUsersWhoHaveNotLoggedInSince(@Param("threshold") LocalDate threshold);

    /** User active đã bỏ ≥2 ngày (có từng học), loại never-studied — cho mail comeback. */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.lastStudyDate IS NOT NULL AND u.lastStudyDate < :threshold")
    List<User> findUsersWithBrokenStreak(@Param("threshold") LocalDate threshold);

    /** User active có hoạt động đúng studyDate yesterday — streak đang nguy hiểm hôm nay. */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.lastStudyDate = :studyDate")
    List<User> findActiveUsersWhoLastStudiedOn(@Param("studyDate") LocalDate studyDate);

    @Modifying
    @Query("UPDATE User u SET u.isPremium = false, u.premiumExpiry = NULL WHERE u.isPremium = true AND u.premiumExpiry IS NOT NULL AND u.premiumExpiry < :today")
    int updateExpiredPremium(@Param("today") LocalDate today);
}
