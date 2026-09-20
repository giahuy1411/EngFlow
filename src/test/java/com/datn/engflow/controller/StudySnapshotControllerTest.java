package com.datn.engflow.controller;

import com.datn.engflow.model.entity.StudyPolicy;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.StudyDayRepository;
import com.datn.engflow.repository.StudyPolicyRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.StreakService;
import com.datn.engflow.service.StudyActivityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudySnapshotControllerTest {
    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void serializesActualServiceSnapshotWithIsoDates() throws Exception {
        var users = mock(UserRepository.class);
        var days = mock(StudyDayRepository.class);
        var policies = mock(StudyPolicyRepository.class);
        var redis = mock(StringRedisTemplate.class);
        LocalDate today = LocalDate.of(2026, 9, 20);
        LocalDate cutover = today.minusDays(2);
        when(policies.findById(1)).thenReturn(Optional.of(new StudyPolicy(1, cutover)));
        when(days.findDates(42L, cutover, today)).thenReturn(List.of(today, cutover));
        var service = new StudyActivityService(users, days, policies, redis,
                Clock.fixed(Instant.parse("2026-09-20T05:00:00Z"), ZoneId.of("UTC")));
        var mvc = MockMvcBuilders.standaloneSetup(new StreakController(mock(StreakService.class), service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        var principal = new UserPrincipal(User.builder().id(42L).isActive(true).build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        String json = mvc.perform(get("/api/streak/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2026-09-20"))
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.studiedToday").value(true))
                .andExpect(jsonPath("$.studiedDays[0]").value("2026-09-18"))
                .andExpect(jsonPath("$.studiedDays[1]").value("2026-09-20"))
                .andReturn().getResponse().getContentAsString();
        Path evidence = Path.of("target", "study-snapshot-contract.json");
        Files.createDirectories(evidence.getParent());
        Files.writeString(evidence, json);
        verify(users, never()).save(any());
    }

    @Test
    void anonymousSnapshotIsUnauthorizedAndDoesNotQueryService() throws Exception {
        SecurityContextHolder.clearContext();
        var service = mock(StudyActivityService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new StreakController(mock(StreakService.class), service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        mvc.perform(get("/api/streak/snapshot")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
