package com.datn.engflow.repository;

import com.datn.engflow.model.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, String> {
    Optional<GameSession> findBySessionIdAndUserIdAndIsActiveTrue(String sessionId, Long userId);

    @Modifying
    @Transactional
    int deleteByStartTimeBeforeAndIsActiveTrue(LocalDateTime cutoff);
}
