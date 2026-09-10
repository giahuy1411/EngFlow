package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.GameSessionRedisDTO;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.DeckRepository;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private DeckWordRepository deckWordRepository;

    @Mock
    private StreakService streakService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private GameService gameService;
    private Clock clock;

    @Captor
    private ArgumentCaptor<GameSessionRedisDTO> sessionCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        gameService = new GameService(deckWordRepository, streakService, redisTemplate, userRepository, deckRepository, clock);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(deckRepository.existsById(any())).thenReturn(true);
    }

    // ---- helpers ----

    private Vocabulary vocab(Long id, String word, String meaning) {
        return Vocabulary.builder()
                .id(id)
                .word(word)
                .meaning(meaning)
                .pronunciation("pron-" + word)
                .audioUrl("http://audio/" + word + ".mp3")
                .build();
    }

    private DeckWord deckWord(Vocabulary v, int order) {
        return DeckWord.builder()
                .id((long) order)
                .vocabulary(v)
                .orderIndex(order)
                .build();
    }

    private List<DeckWord> buildDeckWords(int count) {
        List<DeckWord> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Vocabulary v = vocab((long) i, "word" + i, "meaning" + i);
            list.add(deckWord(v, i));
        }
        return list;
    }

    private User userWithPoints(Long userId, int points) {
        return User.builder()
                .id(userId)
                .username("user" + userId)
                .email("user" + userId + "@test.com")
                .passwordHash("hash")
                .totalPoints(points)
                .build();
    }

    private GameSessionRedisDTO session(String sessionId, Long userId, int totalQuestions, Map<String, String> answerMap) {
        return GameSessionRedisDTO.builder()
                .sessionId(sessionId)
                .userId(userId)
                .deckId(10L)
                .gameType("QUIZ")
                .totalQuestions(totalQuestions)
                .answerMap(answerMap)
                .build();
    }

    private String dailyKey(Long userId) {
        return "game:points:today:" + userId + ":" + LocalDate.now(clock);
    }

    // ---- generateQuiz ----

    @Test
    void generateQuiz_validDeck_returnsSessionWithLimited10() {
        List<DeckWord> deckWords = buildDeckWords(15);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(10L)).thenReturn(deckWords);

        Map<String, Object> actualResult = gameService.generateQuiz(10L, 1L);

        assertThat(actualResult).containsKeys("sessionId", "data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) actualResult.get("data");
        assertThat(data).hasSize(10);

        verify(valueOperations).set(anyString(), sessionCaptor.capture(), anyLong(), eq(TimeUnit.HOURS));
        GameSessionRedisDTO captured = sessionCaptor.getValue();
        assertThat(captured.getAnswerMap()).hasSize(10);
    }

    @Test
    void generateQuiz_responseDoesNotExposeAnswer() {
        List<DeckWord> deckWords = buildDeckWords(3);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(10L)).thenReturn(deckWords);

        Map<String, Object> result = gameService.generateQuiz(10L, 1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
        for (Map<String, Object> q : data) {
            assertThat(q).doesNotContainKey("answer");
            assertThat(q).containsKey("options");
            assertThat(q).containsKey("word");
        }
    }

    @Test
    void generateQuiz_deckNotFound_throwsResourceNotFoundException() {
        when(deckRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> gameService.generateQuiz(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- generateMemoryMatch, typing, listening, mixed ----

    @Test
    void generateMemoryMatch_returnsCardsAndSession() {
        List<DeckWord> deckWords = buildDeckWords(10);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(10L)).thenReturn(deckWords);

        Map<String, Object> result = gameService.generateMemoryMatch(10L, 1L);

        assertThat(result).containsKeys("sessionId", "data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cards = (List<Map<String, Object>>) result.get("data");
        assertThat(cards.size()).isEqualTo(16); // 8 pairs * 2
    }

    @Test
    void generateTyping_returnsBaseList() {
        List<DeckWord> deckWords = buildDeckWords(3);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(10L)).thenReturn(deckWords);

        Map<String, Object> result = gameService.generateTyping(10L, 1L);

        assertThat(result).containsKeys("sessionId", "data");
    }

    @Test
    void generateListening_returnsBaseList() {
        List<DeckWord> deckWords = buildDeckWords(3);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(10L)).thenReturn(deckWords);

        Map<String, Object> result = gameService.generateListening(10L, 1L);
        assertThat(result).containsKeys("sessionId", "data");
    }

    @Test
    void generateMixed_returnsBaseList() {
        List<DeckWord> deckWords = buildDeckWords(3);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(10L)).thenReturn(deckWords);

        Map<String, Object> result = gameService.generateMixed(10L, 1L);
        assertThat(result).containsKeys("sessionId", "data");
    }

    // ---- submitGameResult ----

    @Test
    void submitGameResult_sessionNotFound_throwsBadRequestException() {
        when(valueOperations.get("game:session:sessX")).thenReturn(null);

        assertThatThrownBy(() -> gameService.submitGameResult(1L, "sessX", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session không hợp lệ");
    }

    @Test
    void submitGameResult_userIdMismatch_throwsBadRequestException() {
        GameSessionRedisDTO stored = session("sess1", 2L, 5, Map.of("1", "meaning1"));
        when(valueOperations.get("game:session:sess1")).thenReturn(stored);

        assertThatThrownBy(() -> gameService.submitGameResult(1L, "sess1", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session không thuộc về người dùng này");
    }

    @Test
    void submitGameResult_withUserAnswers_validatesServerSide_caseInsensitive() {
        Map<String, String> answerMap = new HashMap<>();
        answerMap.put("1", "hello");
        answerMap.put("2", "world");
        GameSessionRedisDTO stored = session("sess2", 1L, 2, answerMap);
        when(valueOperations.get("game:session:sess2")).thenReturn(stored);
        when(valueOperations.get(dailyKey(1L))).thenReturn(null);
        User user = userWithPoints(1L, 10);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete("game:session:sess2")).thenReturn(true);
        when(valueOperations.increment(eq(dailyKey(1L)), anyLong())).thenReturn(2L);
        when(redisTemplate.expire(eq(dailyKey(1L)), eq(25L), eq(TimeUnit.HOURS))).thenReturn(true);

        List<Map<String, Object>> userAnswers = List.of(
                Map.of("vocabId", 1, "answer", "  HELLO  "),
                Map.of("vocabId", 2, "answer", "world"),
                Map.of("vocabId", 3, "answer", "missing vocab should be ignored")
        );

        Map<String, Object> actualResult = gameService.submitGameResult(1L, "sess2", 0, userAnswers);

        assertThat(actualResult.get("correctAnswers")).isEqualTo(2);
        assertThat(actualResult.get("totalQuestions")).isEqualTo(2);
        verify(userRepository).save(user);
        assertThat(user.getTotalPoints()).isEqualTo(12);
    }

    @Test
    void submitGameResult_serverValidation_overridesManipulatedClientCount() {
        Map<String, String> answerMap = Map.of("1", "a", "2", "b");
        GameSessionRedisDTO stored = session("sess-manip", 1L, 2, answerMap);
        when(valueOperations.get("game:session:sess-manip")).thenReturn(stored);
        when(valueOperations.get(dailyKey(1L))).thenReturn(null);
        User user = userWithPoints(1L, 0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(valueOperations.increment(eq(dailyKey(1L)), anyLong())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // client claims 2/2 but only answered 1 correctly — server should give 1
        List<Map<String, Object>> userAnswers = List.of(
                Map.of("vocabId", 1, "answer", "a"),
                Map.of("vocabId", 2, "answer", "wrong")
        );
        Map<String, Object> result = gameService.submitGameResult(1L, "sess-manip", 2, userAnswers);
        assertThat(result.get("correctAnswers")).isEqualTo(1);
    }

    @Test
    void submitGameResult_fallbackClientCorrect_whenAnswerMapNull_trustsClient() {
        GameSessionRedisDTO stored = GameSessionRedisDTO.builder()
                .sessionId("sess4")
                .userId(1L)
                .deckId(10L)
                .gameType("TYPING")
                .totalQuestions(5)
                .answerMap(null)
                .build();
        when(valueOperations.get("game:session:sess4")).thenReturn(stored);
        when(valueOperations.get(dailyKey(1L))).thenReturn(null);
        User user = userWithPoints(1L, 0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(valueOperations.increment(eq(dailyKey(1L)), anyLong())).thenReturn(3L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        Map<String, Object> actualResult = gameService.submitGameResult(1L, "sess4", 3);
        assertThat(actualResult.get("correctAnswers")).isEqualTo(3);
        verify(streakService).checkin(1L, 5, 1);
    }

    @Test
    void submitGameResult_clientCorrectExceedsTotal_throwsBadRequestException() {
        GameSessionRedisDTO stored = GameSessionRedisDTO.builder()
                .sessionId("sess5")
                .userId(1L)
                .deckId(10L)
                .gameType("QUIZ")
                .totalQuestions(4)
                .answerMap(Map.of("1", "a"))
                .build();
        when(valueOperations.get("game:session:sess5")).thenReturn(stored);

        assertThatThrownBy(() -> gameService.submitGameResult(1L, "sess5", 5, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Số câu trả lời đúng không hợp lệ");
    }

    @Test
    void submitGameResult_clientCorrectNegative_throwsBadRequestException() {
        GameSessionRedisDTO stored = session("sess6", 1L, 5, Map.of("1", "a"));
        when(valueOperations.get("game:session:sess6")).thenReturn(stored);

        assertThatThrownBy(() -> gameService.submitGameResult(1L, "sess6", -1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void submitGameResult_dailyCapReached_setsCorrectToZero() {
        GameSessionRedisDTO stored = session("sess7", 1L, 5, null);
        when(valueOperations.get("game:session:sess7")).thenReturn(stored);
        when(valueOperations.get(dailyKey(1L))).thenReturn(100);
        User user = userWithPoints(1L, 50);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(valueOperations.increment(eq(dailyKey(1L)), eq(0L))).thenReturn(100L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        Map<String, Object> actualResult = gameService.submitGameResult(1L, "sess7", 5);

        assertThat(actualResult.get("correctAnswers")).isEqualTo(0);
        assertThat(user.getTotalPoints()).isEqualTo(50);
    }

    @Test
    void submitGameResult_dailyCapPartialCaps() {
        GameSessionRedisDTO stored = session("sess8", 1L, 5, null);
        when(valueOperations.get("game:session:sess8")).thenReturn(stored);
        when(valueOperations.get(dailyKey(1L))).thenReturn(98);
        User user = userWithPoints(1L, 50);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(valueOperations.increment(eq(dailyKey(1L)), eq(2L))).thenReturn(100L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        Map<String, Object> actualResult = gameService.submitGameResult(1L, "sess8", 5);

        assertThat(actualResult.get("correctAnswers")).isEqualTo(2);
        assertThat(user.getTotalPoints()).isEqualTo(52);
    }

    @Test
    void submitGameResult_redisUnavailableForDailyCap_stillSucceeds() {
        GameSessionRedisDTO stored = session("sess9", 1L, 4, null);
        when(valueOperations.get("game:session:sess9")).thenReturn(stored);
        when(valueOperations.get(dailyKey(1L))).thenThrow(new RuntimeException("Redis down"));
        User user = userWithPoints(1L, 0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis down"));

        // increment also fails
        doThrow(new RuntimeException("Redis down")).when(valueOperations).increment(anyString(), anyLong());

        Map<String, Object> result = gameService.submitGameResult(1L, "sess9", 3);
        assertThat(result.get("correctAnswers")).isEqualTo(3);
        assertThat(user.getTotalPoints()).isEqualTo(3);
    }

    @Test
    void submitGameResult_userNotFound_throwsResourceNotFoundException() {
        GameSessionRedisDTO stored = session("sess10", 1L, 2, null);
        when(valueOperations.get("game:session:sess10")).thenReturn(stored);
        when(valueOperations.get(dailyKey(1L))).thenReturn(null);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.submitGameResult(1L, "sess10", 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
