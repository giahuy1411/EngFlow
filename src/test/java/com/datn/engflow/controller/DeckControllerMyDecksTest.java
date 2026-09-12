package com.datn.engflow.controller;

import com.datn.engflow.exception.GlobalExceptionHandler;
import com.datn.engflow.model.dto.response.DeckSummaryResponse;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.DeckService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * audit-v7 F34 regression: GET /api/decks/my is permitAll at the URL layer,
 * so the controller must null-guard the principal instead of NPE-ing into 500.
 */
@ExtendWith(MockitoExtension.class)
class DeckControllerMyDecksTest {

    @Mock
    private DeckService deckService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new DeckController(deckService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void myDecks_withoutPrincipal_returns401notNpe500() throws Exception {
        mockMvc.perform(get("/api/decks/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Vui lòng đăng nhập"));
        verify(deckService, never()).getUserDeckPage(any(), any(), any());
    }

    @Test
    void myDecks_withPrincipal_returnsPage() {
        // Plain call: standalone MockMvc lacks the Spring Data Jackson module,
        // so PageImpl serialization is not the behavior under test here.
        User user = User.builder().id(2L).username("student").email("user@gmail.com")
                .passwordHash("hash").isActive(true).isAdmin(false).build();
        UserPrincipal principal = new UserPrincipal(user);
        DeckSummaryResponse deck = DeckSummaryResponse.builder().id(1L).name("Oxford 3000").wordCount(10).build();
        when(deckService.getUserDeckPage(eq(2L), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deck)));

        var entity = new DeckController(deckService).getUserDecks(principal, null, 0, 9);
        org.assertj.core.api.Assertions.assertThat(entity.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        org.assertj.core.api.Assertions.assertThat(((Page<?>) entity.getBody()).getTotalElements()).isEqualTo(1);
    }

    @Test
    void myDecks_sizeClampedToMax100() {
        User user = User.builder().id(2L).username("student").email("user@gmail.com")
                .passwordHash("hash").isActive(true).isAdmin(false).build();
        UserPrincipal principal = new UserPrincipal(user);
        when(deckService.getUserDeckPage(eq(2L), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        new DeckController(deckService).getUserDecks(principal, null, 0, 99999);

        var captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(deckService).getUserDeckPage(eq(2L), any(), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }
}
