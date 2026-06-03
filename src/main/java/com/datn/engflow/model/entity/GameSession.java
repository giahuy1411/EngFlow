package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "game_sessions", indexes = {
    @Index(name = "idx_game_session_user_active", columnList = "user_id, is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSession {

    @Id
    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "game_type", length = 50, nullable = false)
    private String gameType;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "start_time", updatable = false)
    private LocalDateTime startTime;
    
    @PrePersist
    public void generateId() {
        if (this.sessionId == null) {
            this.sessionId = UUID.randomUUID().toString();
        }
    }
}
