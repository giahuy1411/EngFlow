package com.datn.engflow.service.impl;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.FlashcardReviewRequest;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.UserVocabularyProgress;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.UserVocabularyProgressRepository;
import com.datn.engflow.repository.VocabularyRepository;
import com.datn.engflow.service.FlashcardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashcardServiceImpl implements FlashcardService {

    private final UserVocabularyProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final VocabularyRepository vocabularyRepository;

    @Override
    @Transactional
    public void reviewFlashcard(FlashcardReviewRequest request, String email) {
        log.info("Review flashcard: vocabularyId={}, email={}", request.getVocabularyId(), email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        Vocabulary vocabulary = vocabularyRepository.findById(request.getVocabularyId()).orElseThrow(() -> new ResourceNotFoundException("Vocabulary", "id", request.getVocabularyId()));

        UserVocabularyProgress progress = progressRepository.findByUserIdAndVocabularyId(user.getId(), vocabulary.getId())
                .orElse(UserVocabularyProgress.builder()
                        .user(user)
                        .vocabulary(vocabulary)
                        .masteryLevel(0)
                        .reviewCount(0)
                        .build());

        progress.setReviewCount(progress.getReviewCount() + 1);

        if (Boolean.TRUE.equals(request.getIsKnown())) {
            if (progress.getMasteryLevel() < 3) {
                progress.setMasteryLevel(progress.getMasteryLevel() + 1);
            }
        } else {
            if (progress.getMasteryLevel() > 0) {
                progress.setMasteryLevel(progress.getMasteryLevel() - 1);
            }
        }

        int daysToAdd = switch (progress.getMasteryLevel()) {
            case 0 -> 1;
            case 1 -> 3;
            case 2 -> 7;
            case 3 -> 14;
            default -> 1;
        };

        progress.setNextReviewDate(LocalDateTime.now().plusDays(daysToAdd));
        progressRepository.save(progress);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getStatus(Long vocabularyId, String email) {
        log.info("Lấy trạng thái flashcard: vocabularyId={}, email={}", vocabularyId, email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        
        return progressRepository.findByUserIdAndVocabularyId(user.getId(), vocabularyId)
                .map(UserVocabularyProgress::getMasteryLevel)
                .orElse(0);
    }
}
