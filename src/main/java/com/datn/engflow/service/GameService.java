package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.model.entity.GameSession;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final DeckWordRepository deckWordRepository;
    private final StreakService streakService;
    private final CoinService coinService;
    private final GameSessionRepository gameSessionRepository;

    private GameSession createSession(Long userId, Long deckId, String gameType, int totalQuestions) {
        GameSession session = GameSession.builder()
                .userId(userId)
                .deckId(deckId)
                .gameType(gameType)
                .totalQuestions(totalQuestions)
                .isActive(true)
                .build();
        return gameSessionRepository.save(session);
    }

    @Transactional
    public Map<String, Object> generateQuiz(Long deckId, Long userId) {
        List<DeckWord> deckWords = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deckId);
        List<Vocabulary> allVocabs = deckWords.stream().map(DeckWord::getVocabulary).collect(Collectors.toList());
        List<Map<String, Object>> quiz = new ArrayList<>();

        for (Vocabulary vocab : allVocabs) {
            Map<String, Object> question = new HashMap<>();
            question.put("vocabId", vocab.getId());
            question.put("word", vocab.getWord());
            question.put("pronunciation", vocab.getPronunciation());

            List<String> options = new ArrayList<>();
            options.add(vocab.getMeaning());

            List<Vocabulary> others = new ArrayList<>(allVocabs);
            others.remove(vocab);
            Collections.shuffle(others);

            for (int i = 0; i < Math.min(3, others.size()); i++) {
                options.add(others.get(i).getMeaning());
            }
            Collections.shuffle(options);

            question.put("options", options);
            question.put("answer", vocab.getMeaning());
            quiz.add(question);
        }
        Collections.shuffle(quiz);
        List<Map<String, Object>> finalData = quiz.size() > 10 ? quiz.subList(0, 10) : quiz;

        GameSession session = createSession(userId, deckId, "QUIZ", finalData.size());

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("data", finalData);
        return response;
    }

    @Transactional
    public Map<String, Object> generateMemoryMatch(Long deckId, Long userId) {
        List<DeckWord> deckWords = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deckId);
        List<Vocabulary> allVocabs = deckWords.stream().map(DeckWord::getVocabulary).collect(Collectors.toList());
        Collections.shuffle(allVocabs);

        int pairs = Math.min(8, allVocabs.size());
        List<Vocabulary> selected = allVocabs.subList(0, pairs);

        List<Map<String, Object>> cards = new ArrayList<>();
        for (Vocabulary v : selected) {
            Map<String, Object> wordCard = new HashMap<>();
            wordCard.put("id", v.getId() + "-word");
            wordCard.put("pairId", v.getId());
            wordCard.put("content", v.getWord());
            wordCard.put("type", "word");
            cards.add(wordCard);

            Map<String, Object> meaningCard = new HashMap<>();
            meaningCard.put("id", v.getId() + "-meaning");
            meaningCard.put("pairId", v.getId());
            meaningCard.put("content", v.getMeaning());
            meaningCard.put("type", "meaning");
            cards.add(meaningCard);
        }
        Collections.shuffle(cards);

        GameSession session = createSession(userId, deckId, "MEMORY_MATCH", pairs);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("data", cards);
        return response;
    }

    @Transactional
    public Map<String, Object> generateTyping(Long deckId, Long userId) {
        return generateBaseList(deckId, userId, "TYPING");
    }

    @Transactional
    public Map<String, Object> generateListening(Long deckId, Long userId) {
        return generateBaseList(deckId, userId, "LISTENING");
    }

    @Transactional
    public Map<String, Object> generateMixed(Long deckId, Long userId) {
        return generateBaseList(deckId, userId, "MIXED");
    }

    private Map<String, Object> generateBaseList(Long deckId, Long userId, String gameType) {
        List<DeckWord> deckWords = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deckId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DeckWord dw : deckWords) {
            Vocabulary v = dw.getVocabulary();
            Map<String, Object> item = new HashMap<>();
            item.put("vocabId", v.getId());
            item.put("word", v.getWord());
            item.put("definitionVi", v.getMeaning());
            item.put("pronunciation", v.getPronunciation());
            item.put("audioUrl", v.getAudioUrl());
            result.add(item);
        }
        Collections.shuffle(result);
        List<Map<String, Object>> finalData = result.size() > 10 ? result.subList(0, 10) : result;

        GameSession session = createSession(userId, deckId, gameType, finalData.size());

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("data", finalData);
        return response;
    }

    @Transactional
    public Map<String, Object> submitGameResult(Long userId, String sessionId, int correctAnswers) {
        GameSession session = gameSessionRepository.findBySessionIdAndUserIdAndIsActiveTrue(sessionId, userId)
                .orElseThrow(() -> new BadRequestException("Session kh\u00f4ng h\u1ee3p l\u1ec7 ho\u1eb7c \u0111\u00e3 h\u1ebft h\u1ea1n"));

        if (correctAnswers > session.getTotalQuestions() || correctAnswers < 0) {
            throw new BadRequestException("S\u1ed1 c\u00e2u tr\u1ea3 l\u1eddi \u0111\u00fang kh\u00f4ng h\u1ee3p l\u1ec7");
        }

        session.setIsActive(false);
        gameSessionRepository.save(session);

        String gameType = session.getGameType();
        int coinsPerCorrect = 2;
        if (gameType.equals("MEMORY_MATCH")) coinsPerCorrect = 1;
        if (gameType.equals("LISTENING") || gameType.equals("TYPING")) coinsPerCorrect = 3;

        int earnedCoins = correctAnswers * coinsPerCorrect;
        coinService.earnCoins(userId, earnedCoins);
        streakService.checkin(userId, session.getTotalQuestions(), 1, earnedCoins);

        Map<String, Object> result = new HashMap<>();
        result.put("correctAnswers", correctAnswers);
        result.put("totalQuestions", session.getTotalQuestions());
        result.put("earnedCoins", earnedCoins);

        return result;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        int deleted = gameSessionRepository.deleteByStartTimeBeforeAndIsActiveTrue(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} expired game sessions", deleted);
        }
    }
}
