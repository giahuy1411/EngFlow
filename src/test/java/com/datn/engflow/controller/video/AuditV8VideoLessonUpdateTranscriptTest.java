package com.datn.engflow.controller.video;

import com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.VideoLesson;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VideoLessonRepository;
import com.datn.engflow.security.UserPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Doc transcript tu cot JSON ra record de so sanh theo ngu nghia (bo qua format/thu tu field). */
    private static List<TranscriptLine> transcriptOf(VideoLesson lesson) throws Exception {
        return MAPPER.readValue(lesson.getTranscriptJson(), new TypeReference<List<TranscriptLine>>() { });
    }

    private void assertKeepsSeedTranscript(Long id, String expectedTitle) throws Exception {
        VideoLesson after = videoLessonRepository.findById(id).orElseThrow();
        assertThat(after.getTitle()).isEqualTo(expectedTitle);
        List<TranscriptLine> lines = transcriptOf(after);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).textEn()).isEqualTo("Hello there.");
        assertThat(lines.get(1).textEn()).isEqualTo("One coffee please.");
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

        assertKeepsSeedTranscript(id, "ZZ keep transcript v2");
        assertThat(videoLessonRepository.findById(id).orElseThrow().getCategory()).isEqualTo("KEEP");
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

        assertKeepsSeedTranscript(id, "ZZ keep transcript v3");
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

        assertKeepsSeedTranscript(id, "ZZ keep transcript v4");
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
