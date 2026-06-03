package com.datn.engflow.service;

import java.util.List;
import java.util.Map;

public interface SrsService {
    void reviewWord(Long userId, Long vocabId, int quality); // SM-2 calculation
    List<Map<String, Object>> getDueWords(Long userId, Long deckId);
    Map<String, Object> getStudyStats(Long userId);
}
