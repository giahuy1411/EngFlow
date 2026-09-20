package com.datn.engflow.controller;

import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.model.enums.SkillType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * audit-v10 F115 — cùng họ lỗi với F88/F89/F105.
 *
 * <p>audit-v8 F88 chặn đọc nội dung bài nháp qua {@code GET /api/lessons/{id}/exercises}
 * và {@code /exercises/content}; audit-v9 F105 chặn nốt {@code grade}/{@code submit}.
 * Nhưng {@code LessonSubmissionController} ({@code /api/lesson-submissions/**}) chưa
 * từng đi qua {@code LessonService.assertLessonVisible}: một student biết id bài nháp
 * vẫn nộp được bài (ghi row PENDING vào {@code lesson_submissions}) và đọc lại được
 * nội dung đã nộp của bài nháp đó.
 *
 * <p>Đây không phải rò rỉ đáp án như F105, nhưng cùng bản chất: nội dung chưa publish
 * không được tương tác được với người thường. Chốt: 404 cho student trên bài nháp,
 * 200 cho admin (admin cần preview/chấm).
 */
@SpringBootTest
@Transactional
class AuditV10DraftLessonSubmissionGuardTest {

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
                .username(admin ? "zzsubadmin" : "zzsubstudent")
                .email(admin ? "zzsubadmin@test.com" : "zzsubstudent@test.com")
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
                .title("ZZ submission " + (published ? "pub" : "draft"))
                .content("<p>noi dung</p>")
                .level(LessonLevel.ELEMENTARY)
                .isPublished(published)
                .build());
    }

    private String body(Long lessonId) {
        return "{\"lessonId\":" + lessonId + ",\"skillType\":\"WRITING\",\"submissionText\":\"bai lam\"}";
    }

    @Test
    void draftLesson_submitSkill_404ForStudent() throws Exception {
        Lesson lesson = seedLesson(false);
        mockMvc.perform(post("/api/lesson-submissions/submit")
                        .with(asUser(false))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(lesson.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void draftLesson_readOwnSubmission_404ForStudent() throws Exception {
        Lesson lesson = seedLesson(false);
        mockMvc.perform(get("/api/lesson-submissions/my/lesson/{id}/skill/WRITING", lesson.getId())
                        .with(asUser(false)))
                .andExpect(status().isNotFound());
    }

    @Test
    void draftLesson_submitSkill_adminPreviewAllowed() throws Exception {
        Lesson lesson = seedLesson(false);
        mockMvc.perform(post("/api/lesson-submissions/submit")
                        .with(asUser(true))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(lesson.getId())))
                .andExpect(status().isOk());
    }

    @Test
    void publishedLesson_submitSkill_studentStill200() throws Exception {
        Lesson lesson = seedLesson(true);
        mockMvc.perform(post("/api/lesson-submissions/submit")
                        .with(asUser(false))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(lesson.getId())))
                .andExpect(status().isOk());
    }
}
