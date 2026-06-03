package com.datn.engflow.repository;

import com.datn.engflow.model.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByPointsRequiredLessThanEqual(Integer points);
}
