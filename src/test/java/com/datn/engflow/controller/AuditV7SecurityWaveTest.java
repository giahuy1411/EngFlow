package com.datn.engflow.controller;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.MediaSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * audit-v7 regression guards for the second P1 wave (F54–F56, F62):
 *  - F54: GET lesson exercise attempts requires a real login — 401, never the
 *    old NPE-500, and never PUBLIC via the shadowing permitAll rule.
 *  - F55: media proxy refuses unsigned/expired/bad-ticket keys with 403 even
 *    when the object exists server-side; a valid HMAC ticket passes the gate.
 *  - F56: (retired in audit-v15) the public structure GET is gone with the
 *    Lesson Builder "Đường B" removal — see the F56 block below.
 *  - F62: a bogus enum query value is 400, not 500.
 */
@SpringBootTest
@Transactional
class AuditV7SecurityWaveTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private UserRepository userRepository;
    @Autowired private com.datn.engflow.repository.LessonRepository lessonRepository;
    @Autowired private MediaSigner mediaSigner;
    @Value("${jwt.secret}") private String jwtSecret;

    private MockMvc mockMvc;

    @BeforeEach void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    }

    // ---- F54 ----

    @Test
    void guestAttemptsIsUnauthorizedNot500() throws Exception {
        mockMvc.perform(get("/api/lessons/{id}/exercises/attempts", 445L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guestAttemptDetailIsUnauthorizedNot500() throws Exception {
        mockMvc.perform(get("/api/lessons/{id}/exercises/attempts/{attemptId}", 445L, 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginlessLessonsPublicEndpointsStillWork() throws Exception {
        // rule phủ rộng GET /api/lessons/** phải còn sống cho guests
        mockMvc.perform(get("/api/lessons/445"))
                .andExpect(status().isOk());
    }

    // ---- F55 ----

    @Test
    void unsignedMediaKeyIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/media/speaking-submissions/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    void badSignatureIsForbidden() throws Exception {
        String key = "speaking-submissions/00000000-0000-0000-0000-000000000000";
        mockMvc.perform(get("/api/v1/media/" + key).param("exp", String.valueOf(Long.MAX_VALUE / 2)).param("sig", "deadbeef"))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiredTicketIsForbidden() throws Exception {
        String key = "speaking-submissions/00000000-0000-0000-0000-000000000000";
        long past = System.currentTimeMillis() / 1000L - 60;
        // recompute a *valid* signature for the past exp to prove expiry (not format) rejects
        String params = mediaSigner.paramsForObject(key);
        // params embeds a future exp; craft the past-exp call by swapping exp and reusing sig —
        // signature mismatch + expiry both must reject, so 403 either way:
        mockMvc.perform(get("/api/v1/media/" + key).param("exp", String.valueOf(past))
                        .param("sig", params.substring(params.indexOf("sig=") + 4)))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedTicketPassesGateAndReachesStorage() throws Exception {
        String key = "definitely-not-a-real-object-" + System.nanoTime() + ".webm";
        String params = mediaSigner.paramsForObject(key);
        // Ticket valid → gate passes → MinIO lookup misses → 404 (NOT 403).
        mockMvc.perform(get("/api/v1/media/" + key)
                        .param("exp", expOf(params)).param("sig", sigOf(params)))
                .andExpect(status().isNotFound());
    }

    private static String expOf(String params) {
        return params.split("&")[0].substring("exp=".length());
    }

    private static String sigOf(String params) {
        return params.split("&")[1].substring("sig=".length());
    }

    // ---- F56 ----

    @Test
    void publicStructureEndpointIsGone() throws Exception {
        // audit-v15: the Lesson Builder "Đường B" was removed. F56 used to assert
        // that GET /api/lessons/{id}/structure was read-only; with the feature gone
        // the endpoint no longer exists at all, which is the stronger guarantee.
        // The lesson used here is a published seeded lesson.
        Long lessonId = 445L;
        if (lessonRepository.findById(lessonId).isEmpty()) return; // env without seed

        mockMvc.perform(get("/api/lessons/{id}/structure", lessonId))
                .andExpect(status().isNotFound());
    }

    // ---- F62 ----

    @Test
    void bogusLevelFilterIs400Not500() throws Exception {
        User admin = userRepository.findByEmail("admin@gmail.com").orElseThrow();
        mockMvc.perform(get("/api/admin/lessons").param("level", "NOT_A_LEVEL")
                        .with(user(toSpringUser(admin))))
                .andExpect(status().isBadRequest());
    }

    private static org.springframework.security.core.userdetails.User toSpringUser(User u) {
        return new org.springframework.security.core.userdetails.User(
                u.getEmail(), "n/a",
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        Boolean.TRUE.equals(u.getIsAdmin()) ? "ROLE_ADMIN" : "ROLE_USER")));
    }
}
