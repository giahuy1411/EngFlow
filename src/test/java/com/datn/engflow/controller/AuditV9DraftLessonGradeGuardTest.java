package com.datn.engflow.controller;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * audit-v9 F105. audit-v8 F89 da chan {@code GET /api/lessons/{id}/exercises} va
 * {@code /exercises/content} cua bai nhap (tra 404 cho guest/student) nhung
 * {@code POST /exercises/grade} va {@code POST /exercises/submit} KHONG goi
 * {@code assertLessonVisible}. SecurityConfig chi bat {@code authenticated()} cho 2 route
 * nay nen sinh vien bieu id cua bai nhap van cham duoc va nhan
 * {@code correctAnswer} trong response - leak dap an cua noi dung chua publish.
 *
 * <p>Do live 2026-09-17: draft lesson 102049 / exercise 776281 tra
 * 200 + {"results":[{"correctAnswer":"4",...}]} cho student token.
 *
 * <p>Chot lai: grade/submit phai dung cung guard {@code assertLessonVisible} nhu
 * 2 endpoint doc - bai nhap tra 404 cho student, admin van grade duoc de preview.
 */
@SpringBootTest
@Transactional
class AuditV9DraftLessonGradeGuardTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private RequestPostProcessor asUser(boolean admin) {
        User saved = userRepository.save(User.builder()
                .username(admin ? "zzgradeadmin" : "zzgradestudent")
                .email(admin ? "zzgradeadmin@test.com" : "zzgradestudent@test.com")
                .passwordHash("not-a-real-hash").isAdmin(admin).build());
        UserPrincipal principal = new UserPrincipal(saved);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setUserPrincipal(authentication);
            return request;
        };
    }

    private Lesson seedLesson(boolean published) {
        Lesson lesson = Lesson.builder()
                .title("ZZ grade " + (published ? "pub" : "draft"))
                .content("<p>noi dung nhap</p>")
                .level(LessonLevel.ELEMENTARY)
                .isPublished(published)
                .build();
        Lesson saved = lessonRepository.save(lesson);
        exerciseRepository.save(Exercise.builder()
                .lesson(saved)
                .question("2 + 2 = ?")
                .options("[\"5\",\"4\"]")
                .correctAnswer("4")
                .exerciseType(ExerciseType.MULTIPLE_CHOICE)
                .difficulty(ExerciseDifficulty.EASY)
                .build());
        return saved;
    }

    private Long exerciseIdOf(Lesson lesson) {
        return exerciseRepository.findByLessonIdOrderByOrderIndexAsc(lesson.getId()).get(0).getId();
    }

    @Test
    void draftLesson_grade_404ForStudent() throws Exception {
        Lesson lesson = seedLesson(false);
        mockMvc.perform(post("/api/lessons/{id}/exercises/grade", lesson.getId())
                        .with(asUser(false))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"exerciseId\":" + exerciseIdOf(lesson) + ",\"userAnswer\":\"4\"}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void draftLesson_submit_404ForStudent() throws Exception {
        Lesson lesson = seedLesson(false);
        mockMvc.perform(post("/api/lessons/{id}/exercises/submit", lesson.getId())
                        .with(asUser(false))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"exerciseId\":" + exerciseIdOf(lesson) + ",\"userAnswer\":\"4\"}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void draftLesson_grade_adminPreviewAllowed() throws Exception {
        Lesson lesson = seedLesson(false);
        mockMvc.perform(post("/api/lessons/{id}/exercises/grade", lesson.getId())
                        .with(asUser(true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"exerciseId\":" + exerciseIdOf(lesson) + ",\"userAnswer\":\"4\"}]}"))
                .andExpect(status().isOk());
    }

    @Test
    void publishedLesson_grade_studentStill200() throws Exception {
        Lesson lesson = seedLesson(true);
        mockMvc.perform(post("/api/lessons/{id}/exercises/grade", lesson.getId())
                        .with(asUser(false))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"exerciseId\":" + exerciseIdOf(lesson) + ",\"userAnswer\":\"4\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].correct").value(true));
    }

    @Test
    void publishedLesson_submit_studentStill200() throws Exception {
        Lesson lesson = seedLesson(true);
        mockMvc.perform(post("/api/lessons/{id}/exercises/submit", lesson.getId())
                        .with(asUser(false))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"exerciseId\":" + exerciseIdOf(lesson) + ",\"userAnswer\":\"4\"}]}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
