package com.datn.engflow.service;

import java.util.List;
import java.util.Map;

public interface GameService {
    Map<String, Object> generateQuiz(Long deckId, Long userId);
    Map<String, Object> generateMemoryMatch(Long deckId, Long userId);
    Map<String, Object> generateTyping(Long deckId, Long userId);
    Map<String, Object> generateListening(Long deckId, Long userId);
    Map<String, Object> generateMixed(Long deckId, Long userId);
    Map<String, Object> submitGameResult(Long userId, String sessionId, int correctAnswers);
}
