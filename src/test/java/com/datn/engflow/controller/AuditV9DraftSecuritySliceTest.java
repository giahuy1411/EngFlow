package com.datn.engflow.controller;

import com.datn.engflow.config.SecurityConfig;
import com.datn.engflow.exception.GlobalExceptionHandler;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.dto.response.GradeResponse;
import com.datn.engflow.security.CustomUserDetailsService;
import com.datn.engflow.security.JwtAuthenticationFilter;
import com.datn.engflow.security.JwtTokenProvider;
import com.datn.engflow.service.ExerciseService;
import com.datn.engflow.service.LessonContentService;
import com.datn.engflow.service.LessonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonExerciseController.class)
@ContextConfiguration(classes = {LessonExerciseController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AuditV9DraftSecuritySliceTest {
    @Autowired
    private org.springframework.web.context.WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void configureSecurityIntegration() {
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @MockitoBean
    private ExerciseService exerciseService;

    @MockitoBean
    private LessonContentService lessonContentService;

    @MockitoBean
    private LessonService lessonService;

    @MockitoBean
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @ParameterizedTest
    @ValueSource(strings = {"grade", "submit"})
    void anonymousRequestsAre401BeforeServices(String action) throws Exception {
        mvc.perform(post("/api/lessons/102049/exercises/" + action)
                        .contentType(APPLICATION_JSON).content("{\"answers\":[]}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(lessonService, exerciseService);
    }

    @WithMockUser(username = "student", roles = "USER")
    @ParameterizedTest
    @ValueSource(strings = {"grade", "submit"})
    void studentDraftRequestsAre404BeforeGrading(String action) throws Exception {
        doThrow(new ResourceNotFoundException("Lesson", "id", 102049L))
                .when(lessonService).assertLessonVisible(102049L, false);

        mvc.perform(post("/api/lessons/102049/exercises/" + action)
                        .contentType(APPLICATION_JSON).content("{\"answers\":[]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(lessonService).assertLessonVisible(102049L, false);
        verifyNoInteractions(exerciseService);
    }

    @WithMockUser(username = "admin", roles = "ADMIN")
    @ParameterizedTest
    @ValueSource(strings = {"grade", "submit"})
    void adminPreviewRemainsAllowed(String action) throws Exception {
        GradeResponse response = GradeResponse.builder().results(List.of()).build();
        if (action.equals("grade")) {
            when(exerciseService.gradeExercises(eq(102049L), any(GradeRequest.class))).thenReturn(response);
        } else {
            when(exerciseService.submitExercises(eq(102049L), any(GradeRequest.class), eq("admin")))
                    .thenReturn(response);
        }

        mvc.perform(post("/api/lessons/102049/exercises/" + action)
                        .contentType(APPLICATION_JSON).content("{\"answers\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());

        verify(lessonService).assertLessonVisible(102049L, true);
    }
}
