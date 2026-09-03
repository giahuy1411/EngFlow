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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
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

    @InjectMocks
    private GameService gameService;

    @Captor
    private ArgumentCaptor<GameSessionRedisDTO> sessionCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @BeforeEach
    void setUp() {
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

    // ---- generateQuiz ----

    @Test
    void generateQuiz_validDeck_returnsSessionWithLimited10() {
        // Given
        List<DeckWord> deckWords = buildDeckWords(15);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(10L)).thenReturn(deckWords);

        // When
        Map<String, Object> actualResult = gameService.generateQuiz(10L, 1L);

        // Then
        assertThat(actualResult).containsKeys("sessionId", "data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) actualResult.get("data");
        assertThat(data).hasSize(10);

        // Verify Redis session stored with correct answer map
        verify(valueOperations).set(anyString(), sessionCaptor.capture(), eq(24L), eq(TimeUnit.HOURS));
        GameSessionRedisDTO captured = sessionCaptor.getValue();
        assertThat(captured.getUserId()).isEqualTo(1L);
        assertThat(captured.getDeckId()).isEqualTo(10L);
        assertThat(captured.getGameType()).isEqualTo("QUIZ");
        assertThat(captured.getTotalQuestions()).isEqualTo(10);
        assertThat(captured.getAnswerMap()).hasSize(10);
    }

    @Test
    void generateQuiz_lessThan10_returnsAllWords() {
        // Given
        List<DeckWord> deckWords = buildDeckWords(3);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(5L)).thenReturn(deckWords);

        // When
        Map<String, Object> actualResult = gameService.generateQuiz(5L, 2L);

        // Then
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) actualResult.get("data");
        assertThat(data).hasSize(3);
        verify(valueOperations).set(anyString(), any(GameSessionRedisDTO.class), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void generateQuiz_emptyDeck_returnsEmptyData() {
        // Given
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(99L)).thenReturn(List.of());

        // When
        Map<String, Object> actualResult = gameService.generateQuiz(99L, 1L);

        // Then
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) actualResult.get("data");
        assertThat(data).isEmpty();
        verify(valueOperations).set(anyString(), sessionCaptor.capture(), eq(24L), eq(TimeUnit.HOURS));
        assertThat(sessionCaptor.getValue().getTotalQuestions()).isZero();
    }

    // ---- generateMemoryMatch ----

    @Test
    void generateMemoryMatch_validDeck_returnsPairedCards() {
        // Given
        List<DeckWord> deckWords = buildDeckWords(10);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(20L)).thenReturn(deckWords);

        // When
        Map<String, Object> actualResult = gameService.generateMemoryMatch(20L, 1L);

        // Then
        assertThat(actualResult).containsKeys("sessionId", "data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cards = (List<Map<String, Object>>) actualResult.get("data");
        // 8 pairs -> 16 cards (Math.min 8)
        assertThat(cards).hasSize(16);
        verify(valueOperations).set(anyString(), sessionCaptor.capture(), eq(24L), eq(TimeUnit.HOURS));
        GameSessionRedisDTO captured = sessionCaptor.getValue();
        assertThat(captured.getGameType()).isEqualTo("MEMORY_MATCH");
        assertThat(captured.getTotalQuestions()).isEqualTo(8);
        assertThat(captured.getAnswerMap()).hasSize(8);
    }

    @Test
    void generateMemoryMatch_smallDeck_limitsPairs() {
        // Given
        List<DeckWord> deckWords = buildDeckWords(2);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(21L)).thenReturn(deckWords);

        // When
        Map<String, Object> actualResult = gameService.generateMemoryMatch(21L, 1L);

        // Then
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cards = (List<Map<String, Object>>) actualResult.get("data");
        assertThat(cards).hasSize(4);
    }

    // ---- generateTyping / Listening / Mixed via base list ----

    @Test
    void generateTyping_validDeck_returnsBaseListSession() {
        // Given
        List<DeckWord> deckWords = buildDeckWords(5);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(30L)).thenReturn(deckWords);

        // When
        Map<String, Object> actualResult = gameService.generateTyping(30L, 1L);

        // Then
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) actualResult.get("data");
        assertThat(data).hasSize(5);
        assertThat(data.get(0)).containsKeys("vocabId", "word", "definitionVi");
        verify(valueOperations).set(anyString(), sessionCaptor.capture(), eq(24L), eq(TimeUnit.HOURS));
        assertThat(sessionCaptor.getValue().getGameType()).isEqualTo("TYPING");
    }

    @Test
    void generateListening_validDeck_returnsListeningSession() {
        // Given
        List<DeckWord> deckWords = buildDeckWords(12);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(31L)).thenReturn(deckWords);

        // When
        Map<String, Object> actualResult = gameService.generateListening(31L, 1L);

        // Then
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) actualResult.get("data");
        assertThat(data).hasSize(10);
        verify(valueOperations).set(anyString(), sessionCaptor.capture(), eq(24L), eq(TimeUnit.HOURS));
        assertThat(sessionCaptor.getValue().getGameType()).isEqualTo("LISTENING");
    }

    @Test
    void generateMixed_limitsTo10AndStoresAnswerMap() {
        // Given
        List<DeckWord> deckWords = buildDeckWords(11);
        when(deckWordRepository.findByDeckIdOrderByOrderIndexAsc(32L)).thenReturn(deckWords);

        // When
        Map<String, Object> actualResult = gameService.generateMixed(32L, 1L);

        // Then
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) actualResult.get("data");
        assertThat(data).hasSize(10);
        verify(valueOperations).set(anyString(), sessionCaptor.capture(), eq(24L), eq(TimeUnit.HOURS));
        assertThat(sessionCaptor.getValue().getGameType()).isEqualTo("MIXED");
        assertThat(sessionCaptor.getValue().getAnswerMap()).hasSize(10);
    }

    // ---- submitGameResult ----

    @Test
    void submitGameResult_sessionNotFound_throwsBadRequestException() {
        // Given
        when(valueOperations.get("game:session:missing")).thenReturn(null);

        // When-Then
        assertThatThrownBy(() -> gameService.submitGameResult(1L, "missing", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session không hợp lệ");
    }

    @Test
    void submitGameResult_userIdMismatch_throwsBadRequestException() {
        // Given
        GameSessionRedisDTO stored = session("sess1", 2L, 5, Map.of("1", "meaning1"));
        when(valueOperations.get("game:session:sess1")).thenReturn(stored);

        // When-Then
        assertThatThrownBy(() -> gameService.submitGameResult(1L, "sess1", 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session không thuộc về người dùng này");
    }

    @Test
    void submitGameResult_withUserAnswers_validatesServerSide_caseInsensitive() {
        // Given
        Map<String, String> answerMap = new HashMap<>();
        answerMap.put("1", "hello");
        answerMap.put("2", "world");
        GameSessionRedisDTO stored = session("sess2", 1L, 2, answerMap);
        when(valueOperations.get("game:session:sess2")).thenReturn(stored);
        String dailyKey = "game:points:today:1:" + LocalDate.now();
        when(valueOperations.get(dailyKey)).thenReturn(null);
        User user = userWithPoints(1L, 10);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete("game:session:sess2")).thenReturn(true);
        when(valueOperations.increment(eq(dailyKey), anyLong())).thenReturn(2L);
        when(redisTemplate.expire(eq(dailyKey), eq(25L), eq(TimeUnit.HOURS))).thenReturn(true);

        List<Map<String, Object>> userAnswers = List.of(
                Map.of("vocabId", 1, "answer", "  HELLO  "),
                Map.of("vocabId", 2, "answer", "world"),
                Map.of("vocabId", 3, "answer", "missing vocab should be ignored")
        );

        // When
        Map<String, Object> actualResult = gameService.submitGameResult(1L, "sess2", 0, userAnswers);

        // Then
        assertThat(actualResult.get("correctAnswers")).isEqualTo(2);
        assertThat(actualResult.get("totalQuestions")).isEqualTo(2);
        verify(streakService).checkin(1L, 2, 1);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getTotalPoints()).isEqualTo(12);
        verify(redisTemplate).delete("game:session:sess2");
    }

    @Test
    void submitGameResult_withUserAnswers_partiallyCorrect_countsOnlyMatching() {
        // Given
        Map<String, String> answerMap = Map.of("10", "mean10", "20", "mean20");
        GameSessionRedisDTO stored = GameSessionRedisDTO.builder()
                .sessionId("sess3")
                .userId(5L)
                .deckId(10L)
                .gameType("QUIZ")
                .totalQuestions(2)
                .answerMap(new HashMap<>(answerMap))
                .build();
        when(valueOperations.get("game:session:sess3")).thenReturn(stored);
        String dailyKey = "game:points:today:5:" + LocalDate.now();
        when(valueOperations.get(dailyKey)).thenReturn(null);
        User user = userWithPoints(5L, 0);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(valueOperations.increment(eq(dailyKey), anyLong())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        List<Map<String, Object>> userAnswers = List.of(
                Map.of("vocabId", "10", "answer", "mean10"),
                Map.of("vocabId", "20", "answer", "wrong")
        );

        // When
        Map<String, Object> actualResult = gameService.submitGameResult(5L, "sess3", 99, userAnswers);

        // Then
        assertThat(actualResult.get("correctAnswers")).isEqualTo(1);
    }

    @Test
    void submitGameResult_fallbackClientCorrect_whenAnswerMapNull_trustsClient() {
        // Given
        GameSessionRedisDTO stored = GameSessionRedisDTO.builder()
                .sessionId("sess4")
                .userId(1L)
                .deckId(10L)
                .gameType("TYPING")
                .totalQuestions(5)
                .answerMap(null)
                .build();
        when(valueOperations.get("game:session:sess4")).thenReturn(stored);
        String dailyKey = "game:points:today:1:" + LocalDate.now();
        when(valueOperations.get(dailyKey)).thenReturn(null);
        User user = userWithPoints(1L, 0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(valueOperations.increment(eq(dailyKey), anyLong())).thenReturn(3L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // When
        Map<String, Object> actualResult = gameService.submitGameResult(1L, "sess4", 3);

        // Then
        assertThat(actualResult.get("correctAnswers")).isEqualTo(3);
        verify(streakService).checkin(1L, 5, 1);
    }

    @Test
    void submitGameResult_clientCorrectExceedsTotal_throwsBadRequestException() {
        // Given
        GameSessionRedisDTO stored = GameSessionRedisDTO.builder()
                .sessionId("sess5")
                .userId(1L)
                .deckId(10L)
                .gameType("QUIZ")
                .totalQuestions(4)
                .answerMap(Map.of("1", "a"))
                .build();
        // Provide answers = null to trigger fallback path & validation
        when(valueOperations.get("game:session:sess5")).thenReturn(stored);

        // When-Then - client sends 5 but total is 4
        assertThatThrownBy(() -> gameService.submitGameResult(1L, "sess5", 5, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Số câu trả lời đúng không hợp lệ");
    }

    @Test
    void submitGameResult_clientCorrectNegative_throwsBadRequestException() {
        // Given
        GameSessionRedisDTO stored = session("sess6", 1L, 5, Map.of("1", "a"));
        when(valueOperations.get("game:session:sess6")).thenReturn(stored);

        // When-Then
        assertThatThrownBy(() -> gameService.submitGameResult(1L, "sess6", -1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void submitGameResult_dailyCapReached_setsCorrectToZero() {
        // Given
        GameSessionRedisDTO stored = session("sess7", 1L, 5, null);
        when(valueOperations.get("game:session:sess7")).thenReturn(stored);
        String dailyKey = "game:points:today:1:" + LocalDate.now();
        when(valueOperations.get(dailyKey)).thenReturn(100);
        User user = userWithPoints(1L, 50);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(valueOperations.increment(eq(dailyKey), eq(0L))).thenReturn(100L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // When
        Map<String, Object> actualResult = gameService.submitGameResult(1L, "sess7", 5);

        // Then
        assertThat(actualResult.get("correctAnswers")).isEqualTo(0);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getTotalPoints()).isEqualTo(50);
    }

    @Test
    void submitGameResult_dailyCapPartial_capsCorrectAnswers() {
        // Given
        GameSessionRedisDTO stored = session("sess8", 1L, 10, null);
        when(valueOperations.get("game:session:sess8")).thenReturn(stored);
        String dailyKey = "game:points:today:1:" + LocalDate.now();
        when(valueOperations.get(dailyKey)).thenReturn(95);
        User user = userWithPoints(1L, 0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(valueOperations.increment(eq(dailyKey), eq(5L))).thenReturn(100L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // When - client asks for 10 but only 5 remain until 100 cap
        Map<String, Object> actualResult = gameService.submitGameResult(1L, "sess8", 10);

        // Then
        assertThat(actualResult.get("correctAnswers")).isEqualTo(5);
    }

    @Test
    void submitGameResult_userNotFound_throwsResourceNotFoundException() {
        // Given
        GameSessionRedisDTO stored = session("sess9", 1L, 3, null);
        when(valueOperations.get("game:session:sess9")).thenReturn(stored);
        String dailyKey = "game:points:today:1:" + LocalDate.now();
        when(valueOperations.get(dailyKey)).thenReturn(null);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When-Then
        assertThatThrownBy(() -> gameService.submitGameResult(1L, "sess9", 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void submitGameResult_success_updatesPointsAndDeletesSession() {
        // Given
        GameSessionRedisDTO stored = session("sess10", 7L, 4, null);
        when(valueOperations.get("game:session:sess10")).thenReturn(stored);
        String dailyKey = "game:points:today:7:" + LocalDate.now();
        when(valueOperations.get(dailyKey)).thenReturn(null);
        User user = userWithPoints(7L, 20);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(redisTemplate.delete("game:session:sess10")).thenReturn(true);
        when(valueOperations.increment(eq(dailyKey), eq(3L))).thenReturn(3L);
        when(redisTemplate.expire(eq(dailyKey), eq(25L), eq(TimeUnit.HOURS))).thenReturn(true);

        // When
        Map<String, Object> actualResult = gameService.submitGameResult(7L, "sess10", 3);

        // Then
        assertThat(actualResult.get("correctAnswers")).isEqualTo(3);
        assertThat(actualResult.get("totalQuestions")).isEqualTo(4);
        verify(streakService).checkin(7L, 4, 1);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getTotalPoints()).isEqualTo(23);
        verify(valueOperations).increment(dailyKey, 3);
        verify(redisTemplate).expire(dailyKey, 25, TimeUnit.HOURS);
        verify(redisTemplate).delete("game:session:sess10");
    }
}
