import io, os

test1 = r'''package com.datn.engflow.controller.video;

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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * audit-v8 F88. {@code VideoLessonRequest.transcript} tung mang {@code @NotNull}, trong khi
 * {@code VideoLessonService.update} lai co guard "null/rong = giu nguyen phu de hien co"
 * (audit-v6 F21). Annotation do chay TRUOC service nen guard thanh dead code: PUT tu form
 * admin (bo trong o phu de khi sua) luon 400 "Du lieu dau vao khong hop le".
 *
 * <p>Create van phai chan payload thieu transcript, nhung bang 400 co thong bao ro rang
 * tu {@code VideoLessonService.validateTranscript} thay vi loi validate chung chung.
 */
@SpringBootTest
@Transactional
class AuditV8VideoLessonUpdateTranscriptTest {

    private static final String TRANSCRIPT = "[{\"start\":0.0,\"end\":2.0,\"textEn\":\"Hello there.\"},"
            + "{\"start\":2.0,\"end\":4.0,\"textEn\":\"One coffee please.\"}]";

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
                .username("zzvleditadmin").email("zzvleditadmin@test.com")
                .passwordHash("not-a-real-hash").isAdmin(true).build());
        UserPrincipal principal = new UserPrincipal(saved);
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            return request;
        };
    }

    private Long seedLesson() {
        VideoLesson lesson = VideoLesson.builder()
                .title("ZZ keep transcript")
                .youtubeVideoId("dQw4w9WgXcQ")
                .level(LessonLevel.INTERMEDIATE)
                .category("KEEP")
                .transcriptJson(TRANSCRIPT)
                .durationSeconds(4)
                .isPublished(true)
                .build();
        return videoLessonRepository.save(lesson).getId();
    }

    @Test
    void update_withNullTranscript_keepsExistingTranscript() throws Exception {
        Long id = seedLesson();

        mockMvc.perform(put("/api/v1/admin/video-lessons/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"ZZ keep transcript v2\",\"description\":\"d\","
                                + "\"youtubeUrl\":\"https://youtu.be/dQw4w9WgXcQ\",\"level\":\"INTERMEDIATE\","
                                + "\"transcript\":null}")
                        .with(asAdmin()))
                .andExpect(status().isOk());

        VideoLesson after = videoLessonRepository.findById(id).orElseThrow();
        assertThat(after.getTitle()).isEqualTo("ZZ keep transcript v2");
        assertThat(after.getTranscriptJson()).isEqualTo(TRANSCRIPT);
        assertThat(after.getCategory()).isEqualTo("KEEP");
    }

    @Test
    void update_withoutTranscriptKey_keepsExistingTranscript() throws Exception {
        Long id = seedLesson();

        mockMvc.perform(put("/api/v1/admin/video-lessons/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"ZZ keep transcript v3\",\"description\":\"d\","
                                + "\"youtubeUrl\":\"https://youtu.be/dQw4w9WgXcQ\",\"level\":\"INTERMEDIATE\"}")
                        .with(asAdmin()))
                .andExpect(status().isOk());

        assertThat(videoLessonRepository.findById(id).orElseThrow().getTranscriptJson()).isEqualTo(TRANSCRIPT);
    }

    @Test
    void update_withEmptyTranscriptArray_keepsExistingTranscript() throws Exception {
        Long id = seedLesson();

        mockMvc.perform(put("/api/v1/admin/video-lessons/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"ZZ keep transcript v4\",\"description\":\"d\","
                                + "\"youtubeUrl\":\"https://youtu.be/dQw4w9WgXcQ\",\"level\":\"INTERMEDIATE\","
                                + "\"transcript\":[]}")
                        .with(asAdmin()))
                .andExpect(status().isOk());

        assertThat(videoLessonRepository.findById(id).orElseThrow().getTranscriptJson()).isEqualTo(TRANSCRIPT);
    }

    @Test
    void create_withoutTranscript_returns400NotCreated() throws Exception {
        mockMvc.perform(post("/api/v1/admin/video-lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"ZZ no transcript\",\"description\":\"d\","
                                + "\"youtubeUrl\":\"https://youtu.be/dQw4w9WgXcQ\",\"level\":\"INTERMEDIATE\"}")
                        .with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void create_withSingleLineTranscript_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/video-lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"ZZ one line\",\"description\":\"d\","
                                + "\"youtubeUrl\":\"https://youtu.be/dQw4w9WgXcQ\",\"level\":\"INTERMEDIATE\","
                                + "\"transcript\":[{\"start\":0.0,\"end\":2.0,\"textEn\":\"Only one.\"}]}")
                        .with(asAdmin()))
                .andExpect(status().isBadRequest());
    }
}
'''

test2 = r'''package com.datn.engflow.controller;

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
'''

test3 = r'''package com.datn.engflow.controller.video;

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
'''

files = {
  "src/test/java/com/datn/engflow/controller/video/AuditV8VideoLessonUpdateTranscriptTest.java": test1,
  "src/test/java/com/datn/engflow/controller/AuditV8DraftLessonVisibilityTest.java": test2,
  "src/test/java/com/datn/engflow/controller/video/AuditV8VideoLessonDraftVisibilityTest.java": test3,
}
for path, content in files.items():
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(content.replace("\n", "\r\n"))
    print("wrote", path)
