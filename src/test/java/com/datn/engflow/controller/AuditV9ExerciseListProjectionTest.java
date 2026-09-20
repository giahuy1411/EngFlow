package com.datn.engflow.controller;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * audit-v9 F108 regression. The list endpoint now reads a flat projection
 * ({@code ExerciseLessonProjection}) instead of joining the Lesson entity, so
 * this test pins the response CONTRACT that the projection must still satisfy:
 * lessonId/lessonTitle come from the join, answers stay hidden for a non-admin
 * caller, and rows keep order_index ordering.
 */
@SpringBootTest
@Transactional
class AuditV9ExerciseListProjectionTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private com.datn.engflow.service.ExerciseService exerciseService;

    private MockMvc mockMvc;
    private Long lessonId;

    @BeforeEach
    void seed() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        Lesson lesson = lessonRepository.save(Lesson.builder()
                .title("ZZ projection lesson")
                .content("<p>projection probe content</p>")
                .level(LessonLevel.ELEMENTARY)
                .isPublished(true)
                .build());
        lessonId = lesson.getId();
        exerciseRepository.save(Exercise.builder().lesson(lesson).question("second")
                .options("[\"a\",\"b\"]").correctAnswer("SECRET2")
                .exerciseType(ExerciseType.MULTIPLE_CHOICE).difficulty(ExerciseDifficulty.EASY)
                .orderIndex(2).build());
        exerciseRepository.save(Exercise.builder().lesson(lesson).question("first")
                .options("[\"a\",\"b\"]").correctAnswer("SECRET1")
                .exerciseType(ExerciseType.MULTIPLE_CHOICE).difficulty(ExerciseDifficulty.EASY)
                .orderIndex(1).build());
    }

    @Test
    void listKeepsContract_andHidesAnswers() throws Exception {
        mockMvc.perform(get("/api/lessons/{id}/exercises", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // order_index ordering, not insertion order
                .andExpect(jsonPath("$[0].question").value("first"))
                .andExpect(jsonPath("$[1].question").value("second"))
                // join columns still present
                .andExpect(jsonPath("$[0].lessonId").value(lessonId))
                .andExpect(jsonPath("$[0].lessonTitle").value("ZZ projection lesson"))
                .andExpect(jsonPath("$[0].exerciseType").value("MULTIPLE_CHOICE"))
                .andExpect(jsonPath("$[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$[0].options").value("[\"a\",\"b\"]"))
                // and the answer key must NOT be exposed without includeAnswers
                .andExpect(jsonPath("$[0].correctAnswer").doesNotExist());
    }

    /**
     * The admin includeAnswers branch is exercised at the service level: the
     * MockMvc variant cannot be used because the JWT filter resets the security
     * context, so the controller's ROLE_ADMIN check never sees the mock
     * principal (verified 2026-09-17: expected 200, got 403). The live HTTP path
     * for an admin caller is covered by the p1/p2 API sweep.
     */
    @Test
    void projection_exposesAnswerWhenExplicitlyRequested() {
        var rows = exerciseService.getExercisesByLesson(lessonId, true);
        org.assertj.core.api.Assertions.assertThat(rows).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(rows.get(0).getCorrectAnswer()).isEqualTo("SECRET1");
        org.assertj.core.api.Assertions.assertThat(rows.get(0).getLessonTitle()).isEqualTo("ZZ projection lesson");
    }
}
