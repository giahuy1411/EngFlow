package com.datn.engflow.service;

import com.datn.engflow.model.entity.UserVocabularyProgress;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.repository.UserVocabularyProgressRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VocabularyRepository;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
/**
 * class SrsService.
 */
public class SrsService {

    /**
     * audit-v9 F106: upper bound for the SM-2 interval. Without it every review with
     * {@code quality >= 3} multiplies the stored interval by the ease factor, so the
     * value grows without limit and {@code next_review_date = now().plusDays(interval)}
     * eventually leaves the {@code datetime2} range (max 9999-12-31). Measured live on
     * 2026-09-17: user_vocabulary_progress id=20002 stored
     * {@code srs_interval=1_537_216} (=4210 years) and every further review answered
     * HTTP 500 with "One or more values is out of range of values for the datetime2
     * SQL Server data type" - permanently, because the row is never repaired.
     *
     * <p>365 days is the standard SM-2 ceiling and lets an already-broken row heal
     * itself on the next review.
     */
    private static final int MAX_INTERVAL_DAYS = 365;

    private final UserVocabularyProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final VocabularyRepository vocabularyRepository;
    private final DeckWordRepository deckWordRepository;
    private final StudyActivityService studyActivityService;

    @Transactional
    public void reviewWord(Long userId, Long vocabId, int quality) {
        if (quality < 0 || quality > 5) {
            throw new com.datn.engflow.exception.BadRequestException("quality phải từ 0 đến 5");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Vocabulary vocab = vocabularyRepository.findById(vocabId)
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary", "id", vocabId));

        UserVocabularyProgress progress = progressRepository.findByUserIdAndVocabularyId(userId, vocabId)
                .orElse(UserVocabularyProgress.builder()
                        .user(user)
                        .vocabulary(vocab)
                        .build());

        int repetitions = progress.getRepetitions();
        int interval = progress.getInterval();
        double easeFactor = progress.getEaseFactor();

        if (quality >= 3) {
            if (repetitions == 0) {
                interval = 1;
            } else if (repetitions == 1) {
                interval = 6;
            } else {
                interval = (int) Math.round((double) interval * easeFactor);
            }
            repetitions++;
        } else {
            repetitions = 0;
            interval = 1;
        }

        // Keep the interval (and therefore next_review_date) inside the column range.
        if (interval > MAX_INTERVAL_DAYS) {
            interval = MAX_INTERVAL_DAYS;
        } else if (interval < 0) {
            interval = 1;
        }

        easeFactor = easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        if (easeFactor < 1.3) {
            easeFactor = 1.3;
        }

        progress.setRepetitions(repetitions);
        progress.setInterval(interval);
        progress.setEaseFactor(easeFactor);
        progress.setNextReviewDate(LocalDateTime.now().plusDays(interval));
        progress.setReviewCount(progress.getReviewCount() + 1);
        progress.setUpdatedAt(LocalDateTime.now());

        if (quality == 5) progress.setMasteryLevel(3);
        else if (quality >= 3) progress.setMasteryLevel(2);
        else if (quality >= 1) progress.setMasteryLevel(1);
        else progress.setMasteryLevel(0);

        progressRepository.save(progress);
        studyActivityService.recordStudy(userId);
    }

    public List<Map<String, Object>> getDueWords(Long userId, Long deckId) {
        List<DeckWord> deckWords = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deckId);
        List<Map<String, Object>> dueWords = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (DeckWord dw : deckWords) {
            Vocabulary vocab = dw.getVocabulary();
            Optional<UserVocabularyProgress> progressOpt = progressRepository.findByUserIdAndVocabularyId(userId, vocab.getId());

            boolean isDue = false;
            int level = 0;
            UserVocabularyProgress progress = progressOpt.orElse(null);
            if (progress == null) {
                isDue = true;
            } else {
                level = progress.getMasteryLevel();
                if (progress.getNextReviewDate() == null || progress.getNextReviewDate().isBefore(now) || progress.getNextReviewDate().isEqual(now)) {
                    isDue = true;
                }
            }

            if (isDue) {
                Map<String, Object> map = new HashMap<>();
                map.put("vocabId", vocab.getId());
                map.put("word", vocab.getWord());
                map.put("pronunciation", vocab.getPronunciation());
                map.put("definitionVi", vocab.getMeaning());
                map.put("definitionEn", vocab.getDefinitionEn());
                map.put("exampleSentence", vocab.getExampleSentence());
                map.put("audioUrl", vocab.getAudioUrl());
                map.put("wordType", vocab.getWordType());
                map.put("level", level);
                dueWords.add(map);
            }
        }
        return dueWords;
    }

    public Map<String, Object> getStudyStats(Long userId) {
        List<UserVocabularyProgress> allProgress = progressRepository.findByUserId(userId);
        long newWords = 0;
        long learningWords = 0;
        long masteredWords = 0;

        LocalDateTime now = LocalDateTime.now();
        long dueReviews = 0;

        for (UserVocabularyProgress p : allProgress) {
            if (p.getMasteryLevel() == 0) newWords++;
            else if (p.getMasteryLevel() < 3) learningWords++;
            else masteredWords++;

            if (p.getNextReviewDate() != null && (p.getNextReviewDate().isBefore(now) || p.getNextReviewDate().isEqual(now))) {
                dueReviews++;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("newWords", newWords);
        stats.put("learningWords", learningWords);
        stats.put("masteredWords", masteredWords);
        stats.put("totalWords", allProgress.size());
        stats.put("dueReviews", dueReviews);

        return stats;
    }
}
