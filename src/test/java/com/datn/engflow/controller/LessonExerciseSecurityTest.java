package com.datn.engflow.controller;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards P0: includeAnswers leak via LessonExerciseController.
 * Guest (no token) requesting ?includeAnswers=true must be 403, not 200 with correctAnswer.
 * Normal user without includeAnswers must get correctAnswer=null.
 * Admin with includeAnswers=true must get correctAnswer exposed.
 */
@SpringBootTest
@Transactional
class LessonExerciseSecurityTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private UserRepository userRepository;
    @Autowired private com.datn.engflow.repository.ExerciseRepository exerciseRepository;
    @Value("${jwt.secret}") private String jwtSecret;

    private MockMvc mockMvc;

    /**
     * Lesson 444 was accidentally deleted during audit-v3 (restored as 91900).
     * These security guards must not depend on a hard-coded lesson id: pick
     * any lesson that currently has exercises, fall back to 444 if present.
     */
    private Long lessonWithExercisesId() {
        return exerciseRepository.findAll().stream()
                .map(ex -> ex.getLesson() != null ? ex.getLesson().getId() : null)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(444L);
    }

    @BeforeEach void setup() { mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build(); }

    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Autowired private com.datn.engflow.security.JwtTokenProvider jwtTokenProvider;

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
        String resp = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("token").asText();
    }

    private String tokenViaProvider(String email) {
        // Directly use provider; role claim derived from DB but filter loads authorities from DB anyway
        User u = userRepository.findByEmail(email).orElseThrow();
        return jwtTokenProvider.generateToken(u.getEmail(), Boolean.TRUE.equals(u.getIsAdmin()) ? "ADMIN" : "USER", u.getIsPremium());
    }

    @Test
    void guestIncludeAnswersIsForbidden() throws Exception {
        Long lessonId = lessonWithExercisesId();
        mockMvc.perform(get("/api/lessons/{id}/exercises", lessonId).param("includeAnswers", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userIncludeAnswersIsForbidden() throws Exception {
        Long lessonId = lessonWithExercisesId();
        User anyUser = userRepository.findAll().stream().filter(u -> !Boolean.TRUE.equals(u.getIsAdmin())).findFirst().orElseThrow();
        String tok = tokenViaProvider(anyUser.getEmail());
        mockMvc.perform(get("/api/lessons/{id}/exercises", lessonId).param("includeAnswers", "true").header("Authorization", "Bearer " + tok))
                .andExpect(status().isForbidden());
    }

    @Test
    void guestNormalReturnsNoAnswers() throws Exception {
        Long lessonId = lessonWithExercisesId();
        mockMvc.perform(get("/api/lessons/{id}/exercises", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist());
        // Some implementations return null, so also check null
        mockMvc.perform(get("/api/lessons/{id}/exercises", lessonId))
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    // must contain correctAnswer:null or absent, not a real value
                    if (body.contains("\"correctAnswer\":\"")) {
                        throw new AssertionError("correctAnswer leaked: " + body.substring(0, 300));
                    }
                });
    }

    @Test
    void adminIncludeAnswersExposesCorrectAnswer() throws Exception {
        Long lessonId = lessonWithExercisesId();
        String tok = tokenViaProvider("admin@gmail.com");
        mockMvc.perform(get("/api/lessons/{id}/exercises", lessonId).param("includeAnswers", "true").header("Authorization", "Bearer " + tok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correctAnswer").isNotEmpty());
    }

    @Test
    void userCanGradeExercises() throws Exception {
        // BUG-1 regression guard: ROLE_USER must NOT get 403 on grade endpoint.
        Long lessonId = lessonWithExercisesId();
        User anyUser = userRepository.findAll().stream().filter(u -> !Boolean.TRUE.equals(u.getIsAdmin())).findFirst().orElseThrow();
        String tok = tokenViaProvider(anyUser.getEmail());
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "answers", java.util.List.of(java.util.Map.of("exerciseId", 1, "userAnswer", "A"))));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/lessons/{id}/exercises/grade", lessonId)
                        .header("Authorization", "Bearer " + tok)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void guestCannotGradeExercises() throws Exception {
        Long lessonId = lessonWithExercisesId();
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "answers", java.util.List.of()));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/lessons/{id}/exercises/grade", lessonId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
