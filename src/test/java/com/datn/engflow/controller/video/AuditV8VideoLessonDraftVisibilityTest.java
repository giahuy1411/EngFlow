package com.datn.engflow.controller.video;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.VideoLesson;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VideoLessonRepository;
import com.datn.engflow.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * audit-v8 F89 (nhanh video). {@code GET /api/v1/video-lessons/{id}} permitAll va tra ca
 * transcript cua bai nhap. Do that: tao bai {@code isPublished=false} roi GET khong token
 * -> 200 + transcript 2 dong; endpoint list thi da loc isPublished=true.
 */
@SpringBootTest
@Transactional
class AuditV8VideoLessonDraftVisibilityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoLessonRepository videoLessonRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private RequestPostProcessor asAdmin() {
        User saved = userRepository.save(User.builder()
                .username("zzvldraftadmin").email("zzvldraftadmin@test.com")
                .passwordHash("not-a-real-hash").isAdmin(true).build());
        UserPrincipal principal = new UserPrincipal(saved);
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            return request;
        };
    }

    private Long seedVideoLesson(boolean published) {
        VideoLesson lesson = VideoLesson.builder()
                .title("ZZ video draft " + (published ? "pub" : "unpub"))
                .youtubeVideoId("dQw4w9WgXcQ")
                .level(LessonLevel.ELEMENTARY)
                .transcriptJson("[{\"start\":0.0,\"end\":2.0,\"textEn\":\"Hello there.\"},"
                        + "{\"start\":2.0,\"end\":4.0,\"textEn\":\"One coffee please.\"}]")
                .durationSeconds(4)
                .isPublished(published)
                .build();
        return videoLessonRepository.save(lesson).getId();
    }

    @Test
    void unpublishedVideoLesson_detail_404ForGuest() throws Exception {
        Long id = seedVideoLesson(false);
        mockMvc.perform(get("/api/v1/video-lessons/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void unpublishedVideoLesson_detail_200ForAdmin() throws Exception {
        Long id = seedVideoLesson(false);
        mockMvc.perform(get("/api/v1/video-lessons/{id}", id).with(asAdmin())).andExpect(status().isOk());
    }

    @Test
    void publishedVideoLesson_detail_still200ForGuest() throws Exception {
        Long id = seedVideoLesson(true);
        mockMvc.perform(get("/api/v1/video-lessons/{id}", id)).andExpect(status().isOk());
    }
}
