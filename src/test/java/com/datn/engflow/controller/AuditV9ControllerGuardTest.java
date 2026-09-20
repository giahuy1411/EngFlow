package com.datn.engflow.controller;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.dto.response.GradeResponse;
import com.datn.engflow.service.ExerciseService;
import com.datn.engflow.service.LessonContentService;
import com.datn.engflow.service.LessonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditV9ControllerGuardTest {

    @Mock
    private ExerciseService exerciseService;

    @Mock
    private LessonContentService lessonContentService;

    @Mock
    private LessonService lessonService;

    private LessonExerciseController controller;

    private GradeRequest request;

    @BeforeEach
    void setUp() {
        controller = new LessonExerciseController(exerciseService, lessonContentService, lessonService);
        request = new GradeRequest();
        request.setAnswers(List.of());
    }

    private Authentication authentication(boolean admin) {
        var authorities = List.of(new SimpleGrantedAuthority(admin ? "ROLE_ADMIN" : "ROLE_USER"));
        return new UsernamePasswordAuthenticationToken(admin ? "admin" : "student", "credentials", authorities);
    }

    @ParameterizedTest
    @ValueSource(strings = {"grade", "submit"})
    void draftRoutes_return404WithoutCallingExerciseService(String action) throws Exception {
        doThrow(new ResourceNotFoundException("Lesson", "id", 102049L))
                .when(lessonService).assertLessonVisible(102049L, false);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new com.datn.engflow.exception.GlobalExceptionHandler())
                .build();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/lessons/102049/exercises/" + action)
                        .principal(authentication(false))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());

        verify(lessonService).assertLessonVisible(102049L, false);
        verifyNoInteractions(exerciseService);
    }

    @Test
    void gradeExercises_blocksDraftLessonForStudent() {
        doThrow(new ResourceNotFoundException("Lesson", "id", 102049L))
                .when(lessonService).assertLessonVisible(102049L, false);

        assertThatThrownBy(() -> controller.gradeExercises(102049L, request, authentication(false)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(exerciseService);
    }

    @Test
    void submitExercises_blocksDraftLessonForStudent() {
        doThrow(new ResourceNotFoundException("Lesson", "id", 102049L))
                .when(lessonService).assertLessonVisible(102049L, false);

        assertThatThrownBy(() -> controller.submitExercises(102049L, request, authentication(false)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(exerciseService);
    }

    @Test
    void gradeExercises_allowsDraftLessonPreviewForAdmin() {
        GradeResponse response = GradeResponse.builder().score(1).total(1).percentage(100).build();
        when(exerciseService.gradeExercises(102049L, request)).thenReturn(response);

        ResponseEntity<GradeResponse> result =
                controller.gradeExercises(102049L, request, authentication(true));

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isSameAs(response);
        verify(lessonService).assertLessonVisible(102049L, true);
    }

    @Test
    void gradeExercises_allowsPublishedLessonForStudent() {
        GradeResponse response = GradeResponse.builder().score(1).total(1).percentage(100).build();
        when(exerciseService.gradeExercises(651L, request)).thenReturn(response);

        ResponseEntity<GradeResponse> result =
                controller.gradeExercises(651L, request, authentication(false));

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isSameAs(response);
        verify(lessonService).assertLessonVisible(651L, false);
    }

    @Test
    void submitExercises_allowsPublishedLessonForStudent() {
        GradeResponse response = GradeResponse.builder().score(1).total(1).percentage(100).build();
        when(exerciseService.submitExercises(651L, request, "student")).thenReturn(response);

        ResponseEntity<GradeResponse> result =
                controller.submitExercises(651L, request, authentication(false));

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isSameAs(response);
        verify(lessonService).assertLessonVisible(651L, false);
    }

    @Test
    void submitExercises_allowsAdminPreviewAfterVisibilityCheck() {
        GradeResponse response = GradeResponse.builder().score(1).total(1).percentage(100).build();
        when(exerciseService.submitExercises(102049L, request, "admin")).thenReturn(response);

        ResponseEntity<GradeResponse> result =
                controller.submitExercises(102049L, request, authentication(true));

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isSameAs(response);
        var ordered = org.mockito.Mockito.inOrder(lessonService, exerciseService);
        ordered.verify(lessonService).assertLessonVisible(102049L, true);
        ordered.verify(exerciseService).submitExercises(102049L, request, "admin");
    }
}
