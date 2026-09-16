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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * audit-v8 F89. {@code GET /api/lessons/{id}} va {@code GET /api/lessons/{id}/exercises} la
 * permitAll nhung khong he kiem {@code is_published}, trong khi endpoint list da loc
 * {@code isPublished=true}. Do duoc tren DB that: 6 bai nhap tra 200 + content day du cho
 * guest (khong can dang nhap, khong can biet gi them ngoai id).
 *
 * <p>Chot lai: ban nhap tra 404 voi guest/student (404 chu khong 403 de khong xac nhan
 * su ton tai), admin van doc duoc de preview/review.
 */
@SpringBootTest
@Transactional
class AuditV8DraftLessonVisibilityTest {

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
                .username(admin ? "zzdraftadmin" : "zzdraftstudent")
                .email(admin ? "zzdraftadmin@test.com" : "zzdraftstudent@test.com")
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

    private Long seedLesson(boolean published) {
        Lesson lesson = Lesson.builder()
                .title("ZZ draft " + (published ? "pub" : "unpub"))
                .content("<p>noi dung nhap</p>")
                .level(LessonLevel.ELEMENTARY)
                .isPublished(published)
                .build();
        Long id = lessonRepository.save(lesson).getId();
        exerciseRepository.save(Exercise.builder()
                .lesson(lesson)
                .question("ZZ draft question?")
                .options("[\"a\",\"b\"]")
                .correctAnswer("a")
                .exerciseType(ExerciseType.MULTIPLE_CHOICE)
                .difficulty(ExerciseDifficulty.EASY)
                .build());
        return id;
    }

    @Test
    void unpublishedLesson_detail_404ForGuest() throws Exception {
        Long id = seedLesson(false);
        mockMvc.perform(get("/api/lessons/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void unpublishedLesson_detail_200ForAdmin() throws Exception {
        Long id = seedLesson(false);
        mockMvc.perform(get("/api/lessons/{id}", id).with(asUser(true))).andExpect(status().isOk());
    }

    @Test
    void unpublishedLesson_exercises_404ForGuest() throws Exception {
        Long id = seedLesson(false);
        mockMvc.perform(get("/api/lessons/{id}/exercises", id)).andExpect(status().isNotFound());
    }

    @Test
    void unpublishedLesson_cleanContent_404ForGuest() throws Exception {
        Long id = seedLesson(false);
        mockMvc.perform(get("/api/lessons/{id}/exercises/content", id)).andExpect(status().isNotFound());
    }

    @Test
    void unpublishedLesson_exercises_200ForAdmin() throws Exception {
        Long id = seedLesson(false);
        mockMvc.perform(get("/api/lessons/{id}/exercises", id).with(asUser(true))).andExpect(status().isOk());
    }

    @Test
    void publishedLesson_detail_still200ForGuest() throws Exception {
        Long id = seedLesson(true);
        mockMvc.perform(get("/api/lessons/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void publishedLesson_exercises_still200ForGuest() throws Exception {
        Long id = seedLesson(true);
        mockMvc.perform(get("/api/lessons/{id}/exercises", id)).andExpect(status().isOk());
    }
}
