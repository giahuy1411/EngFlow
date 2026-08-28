package com.datn.engflow.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * class GameSessionRedisDTO.
 */
public class GameSessionRedisDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private Long userId;
    private Long deckId;
    private String gameType;
    private Integer totalQuestions;
    // Stores correct answers for server-side validation (vocabId -> meaning or word -> meaning)
    private java.util.Map<String, String> answerMap;
}
