package com.datn.engflow.service;

import com.datn.engflow.config.RedisConstants;
import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.GameSessionRedisDTO;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.DeckRepository;
import com.datn.engflow.repository.DeckWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * class GameService.
 */
public class GameService {

    private final DeckWordRepository deckWordRepository;
    private final StreakService streakService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final com.datn.engflow.repository.UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final Clock clock;

    private void requireDeck(Long deckId) {
        if (deckId == null || !deckRepository.existsById(deckId)) {
            throw new ResourceNotFoundException("Deck", "deckId", deckId);
        }
    }

    private GameSessionRedisDTO createSession(Long userId, Long deckId, String gameType, int totalQuestions, Map<String, String> answerMap) {
        String sessionId = UUID.randomUUID().toString();
        GameSessionRedisDTO session = GameSessionRedisDTO.builder()
                .sessionId(sessionId)
                .userId(userId)
                .deckId(deckId)
                .gameType(gameType)
                .totalQuestions(totalQuestions)
                .answerMap(answerMap)
                .build();

        String key = "game:session:" + sessionId;
        redisTemplate.opsForValue().set(key, session, RedisConstants.GAME_SESSION_TTL.toHours(), TimeUnit.HOURS);
        log.info("Created temporary game session in Redis: {}", sessionId);
        return session;
    }

    private GameSessionRedisDTO createSession(Long userId, Long deckId, String gameType, int totalQuestions) {
        return createSession(userId, deckId, gameType, totalQuestions, null);
    }

    @Transactional
    public Map<String, Object> generateQuiz(Long deckId, Long userId) {
        requireDeck(deckId);
        List<DeckWord> deckWords = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deckId);
        List<Vocabulary> allVocabs = deckWords.stream().map(DeckWord::getVocabulary).collect(Collectors.toList());
        List<Map<String, Object>> quiz = new ArrayList<>();
        Map<String, String> answerMap = new HashMap<>();

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
            answerMap.put(String.valueOf(vocab.getId()), vocab.getMeaning());
            quiz.add(question);
        }
        Collections.shuffle(quiz);
        List<Map<String, Object>> finalData = quiz.size() > 10 ? quiz.subList(0, 10) : quiz;
        // Keep only answerMap entries for selected questions
        Map<String, String> sessionAnswerMap = new HashMap<>();
        for (Map<String, Object> q : finalData) {
            String vid = String.valueOf(q.get("vocabId"));
            sessionAnswerMap.put(vid, answerMap.get(vid));
        }

        GameSessionRedisDTO session = createSession(userId, deckId, "QUIZ", finalData.size(), sessionAnswerMap);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("data", finalData);
        return response;
    }

    @Transactional
    public Map<String, Object> generateMemoryMatch(Long deckId, Long userId) {
        requireDeck(deckId);
        List<DeckWord> deckWords = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deckId);
        List<Vocabulary> allVocabs = deckWords.stream().map(DeckWord::getVocabulary).collect(Collectors.toList());
        Collections.shuffle(allVocabs);

        int pairs = Math.min(8, allVocabs.size());
        List<Vocabulary> selected = allVocabs.subList(0, pairs);

        List<Map<String, Object>> cards = new ArrayList<>();
        Map<String, String> answerMap = new HashMap<>();
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
            answerMap.put(String.valueOf(v.getId()), v.getMeaning());
        }
        Collections.shuffle(cards);

        GameSessionRedisDTO session = createSession(userId, deckId, "MEMORY_MATCH", pairs, answerMap);

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
        requireDeck(deckId);
        List<DeckWord> deckWords = deckWordRepository.findByDeckIdOrderByOrderIndexAsc(deckId);
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, String> answerMap = new HashMap<>();
        for (DeckWord dw : deckWords) {
            Vocabulary v = dw.getVocabulary();
            Map<String, Object> item = new HashMap<>();
            item.put("vocabId", v.getId());
            item.put("word", v.getWord());
            item.put("definitionVi", v.getMeaning());
            item.put("pronunciation", v.getPronunciation());
            item.put("audioUrl", v.getAudioUrl());
            result.add(item);
            answerMap.put(String.valueOf(v.getId()), v.getWord());
        }
        Collections.shuffle(result);
        List<Map<String, Object>> finalData = result.size() > 10 ? result.subList(0, 10) : result;
        Map<String, String> sessionAnswerMap = new HashMap<>();
        for (Map<String, Object> item : finalData) {
            String vid = String.valueOf(item.get("vocabId"));
            sessionAnswerMap.put(vid, answerMap.get(vid));
        }

        GameSessionRedisDTO session = createSession(userId, deckId, gameType, finalData.size(), sessionAnswerMap);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("data", finalData);
        return response;
    }

    @Transactional
    public Map<String, Object> submitGameResult(Long userId, String sessionId, int correctAnswers) {
        return submitGameResult(userId, sessionId, correctAnswers, null);
    }

    @Transactional
    public Map<String, Object> submitGameResult(Long userId, String sessionId, int clientCorrectAnswers, List<Map<String, Object>> userAnswers) {
        String key = "game:session:" + sessionId;
        GameSessionRedisDTO redisSession = (GameSessionRedisDTO) redisTemplate.opsForValue().get(key);

        if (redisSession == null) {
            throw new BadRequestException("Session không hợp lệ hoặc đã hết hạn");
        }

        if (!redisSession.getUserId().equals(userId)) {
            throw new BadRequestException("Session không thuộc về người dùng này");
        }

        int correctAnswers;
        // Server-side validation if answers provided and session has answerMap
        if (userAnswers != null && redisSession.getAnswerMap() != null && !redisSession.getAnswerMap().isEmpty()) {
            correctAnswers = 0;
            Map<String, String> answerMap = redisSession.getAnswerMap();
            for (Map<String, Object> ans : userAnswers) {
                Object vidObj = ans.get("vocabId");
                Object userAnsObj = ans.get("answer");
                if (vidObj == null || userAnsObj == null) continue;
                String vid = String.valueOf(vidObj);
                String expected = answerMap.get(vid);
                if (expected != null && expected.equalsIgnoreCase(String.valueOf(userAnsObj).trim())) {
                    correctAnswers++;
                }
            }
            log.info("Server-validated game score: {}/{} for session {}", correctAnswers, redisSession.getTotalQuestions(), sessionId);
        } else {
            // Fallback: trust client but log warning and apply daily cap to mitigate farming
            if (redisSession.getAnswerMap() != null) {
                log.warn("Game submit without server validation for session {} type {} - trusting client count {}", sessionId, redisSession.getGameType(), clientCorrectAnswers);
            }
            if (clientCorrectAnswers > redisSession.getTotalQuestions() || clientCorrectAnswers < 0) {
                throw new BadRequestException("Số câu trả lời đúng không hợp lệ");
            }
            correctAnswers = clientCorrectAnswers;
        }

        // Daily cap to prevent farming: max 100 points per day via games
        String dailyKey = RedisConstants.GAME_POINTS_KEY_PREFIX + userId + ":" + LocalDate.now(clock);
        Integer already = null;
        try {
            already = (Integer) redisTemplate.opsForValue().get(dailyKey);
        } catch (Exception e) {
            log.warn("Redis unavailable for daily cap check userId={}: {}", userId, e.getMessage());
        }
        int alreadyPoints = already != null ? already : 0;
        if (alreadyPoints >= RedisConstants.GAME_DAILY_LIMIT) {
            correctAnswers = 0;
            log.warn("Daily game points cap reached for user {}", userId);
        } else if (alreadyPoints + correctAnswers > RedisConstants.GAME_DAILY_LIMIT) {
            correctAnswers = RedisConstants.GAME_DAILY_LIMIT - alreadyPoints;
        }

        streakService.checkin(userId, redisSession.getTotalQuestions(), 1);

        // Cập nhật điểm cho user
        com.datn.engflow.model.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setTotalPoints((user.getTotalPoints() == null ? 0 : user.getTotalPoints()) + correctAnswers);
        userRepository.save(user);

        // Update daily counter — fail-open if Redis down
        try {
            redisTemplate.opsForValue().increment(dailyKey, correctAnswers);
            redisTemplate.expire(dailyKey, RedisConstants.GAME_POINTS_TTL.toHours(), TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis unavailable for daily counter update userId={}: {}", userId, e.getMessage());
        }

        // Xóa session khỏi Redis — best-effort
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to delete game session key {}: {}", key, e.getMessage());
        }
        log.info("Submitted game session results and cleared Redis key: {}", sessionId);

        Map<String, Object> result = new HashMap<>();
        result.put("correctAnswers", correctAnswers);
        result.put("totalQuestions", redisSession.getTotalQuestions());

        return result;
    }
}
