package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.FlashcardReviewRequest;

public interface FlashcardService {
    void reviewFlashcard(FlashcardReviewRequest request, String email);
    Integer getStatus(Long vocabularyId, String email);
}
