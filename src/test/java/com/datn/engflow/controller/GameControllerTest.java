package com.datn.engflow.controller;

import com.datn.engflow.exception.GlobalExceptionHandler;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        GameController controller = new GameController(gameService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserPrincipal principal(Long userId) {
        User user = User.builder()
                .id(userId)
                .username("user" + userId)
                .email("user" + userId + "@test.com")
                .passwordHash("hash")
                .isActive(true)
                .isAdmin(false)
                .build();
        return new UserPrincipal(user);
    }

    private RequestPostProcessor auth(Long userId) {
        UserPrincipal p = principal(userId);
        return (MockHttpServletRequest request) -> {
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()));
            return request;
        };
    }

    // ---- quiz ----

    @Test
    void getQuizGame_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/games/quiz/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getQuizGame_authenticated_returns200WithSession() throws Exception {
        // Given
        Map<String, Object> serviceResult = Map.of(
                "sessionId", "sess-123",
                "data", List.of(Map.of("vocabId", 1, "word", "hello"))
        );
        when(gameService.generateQuiz(10L, 1L)).thenReturn(serviceResult);

        // When-Then
        mockMvc.perform(get("/api/games/quiz/10")
                        .with(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess-123"))
                .andExpect(jsonPath("$.data[0].word").value("hello"));

        verify(gameService).generateQuiz(10L, 1L);
    }

    // ---- memory ----

    @Test
    void getMemoryGame_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/games/memory/20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMemoryGame_authenticated_returns200() throws Exception {
        // Given
        Map<String, Object> serviceResult = Map.of(
                "sessionId", "mem-1",
                "data", List.of(Map.of("id", "1-word", "pairId", 1))
        );
        when(gameService.generateMemoryMatch(20L, 2L)).thenReturn(serviceResult);

        // When-Then
        mockMvc.perform(get("/api/games/memory/20")
                        .with(auth(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("mem-1"));
        verify(gameService).generateMemoryMatch(20L, 2L);
    }

    // ---- typing ----

    @Test
    void getTypingGame_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/games/typing/30"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTypingGame_authenticated_returns200() throws Exception {
        Map<String, Object> serviceResult = Map.of("sessionId", "typ-1", "data", List.of());
        when(gameService.generateTyping(30L, 1L)).thenReturn(serviceResult);

        mockMvc.perform(get("/api/games/typing/30")
                        .with(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("typ-1"));
        verify(gameService).generateTyping(30L, 1L);
    }

    // ---- listening ----

    @Test
    void getListeningGame_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/games/listening/31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getListeningGame_authenticated_returns200() throws Exception {
        Map<String, Object> serviceResult = Map.of("sessionId", "lis-1", "data", List.of());
        when(gameService.generateListening(31L, 3L)).thenReturn(serviceResult);

        mockMvc.perform(get("/api/games/listening/31")
                        .with(auth(3L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("lis-1"));
        verify(gameService).generateListening(31L, 3L);
    }

    // ---- mixed ----

    @Test
    void getMixedGame_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/games/mixed/32"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMixedGame_authenticated_returns200() throws Exception {
        Map<String, Object> serviceResult = Map.of("sessionId", "mix-1", "data", List.of());
        when(gameService.generateMixed(32L, 5L)).thenReturn(serviceResult);

        mockMvc.perform(get("/api/games/mixed/32")
                        .with(auth(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("mix-1"));
        verify(gameService).generateMixed(32L, 5L);
    }

    // ---- submit ----

    @Test
    void submitGameResult_unauthenticated_returns401() throws Exception {
        String requestJson = """
                {
                    "sessionId": "sess1",
                    "correctAnswers": 3
                }
                """;
        mockMvc.perform(post("/api/games/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitGameResult_missingSessionId_returns400() throws Exception {
        String requestJson = """
                {
                    "correctAnswers": 2
                }
                """;
        mockMvc.perform(post("/api/games/submit")
                        .with(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Session ID không được để trống"));
    }

    @Test
    void submitGameResult_blankSessionId_returns400() throws Exception {
        String requestJson = """
                {
                    "sessionId": "   ",
                    "correctAnswers": 1
                }
                """;
        mockMvc.perform(post("/api/games/submit")
                        .with(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Session ID không được để trống"));
    }

    @Test
    void submitGameResult_missingCorrectAnswersWithoutAnswersList_returns400() throws Exception {
        String requestJson = """
                {
                    "sessionId": "sess1"
                }
                """;
        mockMvc.perform(post("/api/games/submit")
                        .with(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("correctAnswers không được để trống"));
    }

    @Test
    void submitGameResult_withAnswersList_callsSecurePath_returns200() throws Exception {
        // Given
        Map<String, Object> serviceResult = Map.of("correctAnswers", 2, "totalQuestions", 5);
        when(gameService.submitGameResult(eq(1L), eq("sess-secure"), eq(2), any())).thenReturn(serviceResult);

        String requestJson = """
                {
                    "sessionId": "sess-secure",
                    "correctAnswers": 2,
                    "answers": [
                        {"vocabId": 1, "answer": "hello"},
                        {"vocabId": 2, "answer": "world"}
                    ]
                }
                """;

        // When-Then
        mockMvc.perform(post("/api/games/submit")
                        .with(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctAnswers").value(2))
                .andExpect(jsonPath("$.totalQuestions").value(5));

        verify(gameService).submitGameResult(eq(1L), eq("sess-secure"), eq(2), any());
    }

    @Test
    void submitGameResult_withCorrectAnswersFallback_returns200() throws Exception {
        // Given
        Map<String, Object> serviceResult = Map.of("correctAnswers", 3, "totalQuestions", 5);
        when(gameService.submitGameResult(1L, "sess-fallback", 3)).thenReturn(serviceResult);

        String requestJson = """
                {
                    "sessionId": "sess-fallback",
                    "correctAnswers": 3
                }
                """;

        // When-Then
        mockMvc.perform(post("/api/games/submit")
                        .with(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctAnswers").value(3));

        verify(gameService).submitGameResult(1L, "sess-fallback", 3);
    }

    @Test
    void submitGameResult_withAnswersList_nullCorrectAnswers_defaultsToZero() throws Exception {
        // Given - when client omits correctAnswers but provides answers list, controller defaults to 0
        Map<String, Object> serviceResult = Map.of("correctAnswers", 1, "totalQuestions", 2);
        when(gameService.submitGameResult(eq(1L), eq("sess-null-correct"), eq(0), any())).thenReturn(serviceResult);

        String requestJson = """
                {
                    "sessionId": "sess-null-correct",
                    "answers": [
                        {"vocabId": 1, "answer": "hello"}
                    ]
                }
                """;

        mockMvc.perform(post("/api/games/submit")
                        .with(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctAnswers").value(1));
    }

    @Test
    void submitGameResult_serviceThrowsBadRequest_returns400ViaHandler() throws Exception {
        // Given
        when(gameService.submitGameResult(1L, "bad-sess", 5))
                .thenThrow(new com.datn.engflow.exception.BadRequestException("Session không hợp lệ hoặc đã hết hạn"));

        String requestJson = """
                {
                    "sessionId": "bad-sess",
                    "correctAnswers": 5
                }
                """;

        mockMvc.perform(post("/api/games/submit")
                        .with(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }
}

