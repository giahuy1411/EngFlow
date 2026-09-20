package com.datn.engflow.controller;

import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.LessonLevel;
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
 * audit-v10 F126 — cùng họ với F88 / F105 / F115.
 *
 * <p>F88 chặn đọc nội dung bài nháp qua {@code /exercises} và {@code /exercises/content};
 * F105 chặn nốt {@code grade}/{@code submit}; F115 chặn {@code /api/lesson-submissions/**}.
 * Nhưng {@code GET /api/lessons/{id}/structure} — chính là đường đọc NỘI DUNG BÀI HỌC
 * dưới dạng section/block — chưa từng đi qua {@code LessonService.assertLessonVisible}.
 *
 * <p>Đây là rò rỉ thật, đã đo được trên dữ liệu sống chứ không suy đoán: lesson 10889
 * ({@code is_published = 0}) trả về 8.013 byte nội dung bài học thật cho student, trong
 * khi {@code GET /api/lessons/10889} trả 404. Tức là nội dung bài chưa publish bị đọc
 * được qua một cửa mà mọi cửa khác đều đã đóng.
 *
 * <p>Chốt hành vi: 404 cho student trên bài nháp, 200 cho admin (admin cần preview),
 * và 200 cho student trên bài đã publish.
 */
@SpringBootTest
@Transactional
class AuditV10DraftLessonStructureGuardTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private RequestPostProcessor asUser(boolean admin) {
        User saved = userRepository.save(User.builder()
                .username(admin ? "zzstructadmin" : "zzstructstudent")
                .email(admin ? "zzstructadmin@test.com" : "zzstructstudent@test.com")
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
        return lessonRepository.save(Lesson.builder()
                .title("ZZ structure " + (published ? "pub" : "draft"))
                .content("<p>noi dung</p>")
                .level(LessonLevel.ELEMENTARY)
                .isPublished(published)
                .build());
    }

    @Test
    void draftLesson_structure_404ForStudent() throws Exception {
        Lesson lesson = seedLesson(false);
        mockMvc.perform(get("/api/lessons/{id}/structure", lesson.getId())
                        .with(asUser(false)))
                .andExpect(status().isNotFound());
    }

    @Test
    void draftLesson_structure_404ForAnonymous() throws Exception {
        Lesson lesson = seedLesson(false);
        mockMvc.perform(get("/api/lessons/{id}/structure", lesson.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void draftLesson_structure_adminPreviewAllowed() throws Exception {
        Lesson lesson = seedLesson(false);
        mockMvc.perform(get("/api/lessons/{id}/structure", lesson.getId())
                        .with(asUser(true)))
                .andExpect(status().isOk());
    }

    @Test
    void publishedLesson_structure_studentStill200() throws Exception {
        Lesson lesson = seedLesson(true);
        mockMvc.perform(get("/api/lessons/{id}/structure", lesson.getId())
                        .with(asUser(false)))
                .andExpect(status().isOk());
    }

    @Test
    void publishedLesson_structure_anonymousStill200() throws Exception {
        Lesson lesson = seedLesson(true);
        mockMvc.perform(get("/api/lessons/{id}/structure", lesson.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void adminStructureEndpointStillReachable() throws Exception {
        // Duong /api/admin/lessons/{id}/structure la duong rieng cua admin; sua F126
        // khong duoc lam no hong.
        Lesson lesson = seedLesson(false);
        mockMvc.perform(get("/api/admin/lessons/{id}/structure", lesson.getId())
                        .with(asUser(true)))
                .andExpect(status().isOk());
    }
}
