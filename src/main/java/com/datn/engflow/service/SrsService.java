package com.datn.engflow.service;

import com.datn.engflow.model.entity.UserVocabularyProgress;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.repository.UserVocabularyProgressRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VocabularyRepository;
import com.datn.engflow.repository.DeckWordRepository;
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
public class SrsService {

    private final UserVocabularyProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final VocabularyRepository vocabularyRepository;
    private final DeckWordRepository deckWordRepository;

    @Transactional
    public void reviewWord(Long userId, Long vocabId, int quality) {
        User user = userRepository.findById(userId).orElseThrow();
        Vocabulary vocab = vocabularyRepository.findById(vocabId).orElseThrow();

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
                interval = (int) Math.round(interval * easeFactor);
            }
            repetitions++;
        } else {
            repetitions = 0;
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
            if (progressOpt.isEmpty()) {
                isDue = true;
            } else {
                UserVocabularyProgress progress = progressOpt.get();
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
