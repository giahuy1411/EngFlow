package com.datn.engflow.repository;

import com.datn.engflow.model.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUserId(Long userId);
    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);

    @Query("SELECT ua.achievement.id FROM UserAchievement ua WHERE ua.user.id = :userId")
    List<Long> findAchievementIdsByUserId(@Param("userId") Long userId);
}
