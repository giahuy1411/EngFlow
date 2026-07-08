package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.GameSessionRedisDTO;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.DeckWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final DeckWordRepository deckWordRepository;
    private final StreakService streakService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final com.datn.engflow.repository.UserRepository userRepository;

    private GameSessionRedisDTO createSession(Long userId, Long deckId, String gameType, int totalQuestions) {
        String sessionId = UUID.randomUUID().toString();
        GameSessionRedisDTO session = GameSessionRedisDTO.builder()
                .sessionId(sessionId)
                .userId(userId)
                .deckId(deckId)
                .gameType(gameType)
                .totalQuestions(totalQuestions)
                .build();
        
        String key = "game:session:" + sessionId;
        redisTemplate.opsForValue().set(key, session, 24, TimeUnit.HOURS);
        log.info("Created temporary game session in Redis: {}", sessionId);
        return session;
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

        GameSessionRedisDTO session = createSession(userId, deckId, "QUIZ", finalData.size());

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

        GameSessionRedisDTO session = createSession(userId, deckId, "MEMORY_MATCH", pairs);

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

        GameSessionRedisDTO session = createSession(userId, deckId, gameType, finalData.size());

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getSessionId());
        response.put("data", finalData);
        return response;
    }

    @Transactional
    public Map<String, Object> submitGameResult(Long userId, String sessionId, int correctAnswers) {
        String key = "game:session:" + sessionId;
        GameSessionRedisDTO redisSession = (GameSessionRedisDTO) redisTemplate.opsForValue().get(key);

        if (redisSession == null) {
            throw new BadRequestException("Session không hợp lệ hoặc đã hết hạn");
        }

        if (!redisSession.getUserId().equals(userId)) {
            throw new BadRequestException("Session không thuộc về người dùng này");
        }

        if (correctAnswers > redisSession.getTotalQuestions() || correctAnswers < 0) {
            throw new BadRequestException("Số câu trả lời đúng không hợp lệ");
        }

        streakService.checkin(userId, redisSession.getTotalQuestions(), 1);

        // Cập nhật điểm cho user
        com.datn.engflow.model.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setTotalPoints(user.getTotalPoints() + correctAnswers);
        userRepository.save(user);

        // Xóa session khỏi Redis
        redisTemplate.delete(key);
        log.info("Submitted game session results and cleared Redis key: {}", sessionId);

        Map<String, Object> result = new HashMap<>();
        result.put("correctAnswers", correctAnswers);
        result.put("totalQuestions", redisSession.getTotalQuestions());

        return result;
    }
}
