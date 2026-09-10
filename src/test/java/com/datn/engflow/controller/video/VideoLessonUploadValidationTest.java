package com.datn.engflow.controller.video;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint multipart "/api/v1/admin/video-lessons/upload" nhận meta bằng
 * {@code @RequestPart} nhưng thiếu {@code @Valid}, trong khi đường JSON
 * POST /api/v1/admin/video-lessons đã validate đầy đủ. Payload thiếu title/level
 * vì thế chạy thẳng vào service: {@code request.title().trim()} NPE → 500 mơ hồ,
 * {@code parseLevel(null)} cũng NPE thay vì 400 "Dữ liệu đầu vào không hợp lệ".
 */
@SpringBootTest
@Transactional
class VideoLessonUploadValidationTest {

    private static final String SRT = """
            1
            00:00:01,000 --> 00:00:03,000
            Hello there.

            2
            00:00:03,500 --> 00:00:06,000
            This is a test transcript.
            """;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private RequestPostProcessor asAdmin() {
        User saved = userRepository.save(User.builder()
                .username("zzvideoadmin").email("zzvideoadmin@test.com")
                .passwordHash("not-a-real-hash").isAdmin(true).build());
        UserPrincipal principal = new UserPrincipal(saved);
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            return request;
        };
    }

    private org.springframework.test.web.servlet.RequestBuilder uploadWithMeta(String metaJson) {
        MockMultipartFile meta = new MockMultipartFile(
                "meta", "", "application/json", metaJson.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile transcript = new MockMultipartFile(
                "transcriptFile", "subs.srt", "text/plain", SRT.getBytes(StandardCharsets.UTF_8));
        return multipart("/api/v1/admin/video-lessons/upload")
                .file(meta).file(transcript)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .with(asAdmin());
    }

    @Test
    void upload_missingTitle_returns400Not500() throws Exception {
        mockMvc.perform(uploadWithMeta("""
                {"description":"","youtubeUrl":"https://youtu.be/dQw4w9WgXcQ","level":"INTERMEDIATE","transcript":[]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void upload_missingLevel_returns400Not500() throws Exception {
        mockMvc.perform(uploadWithMeta("""
                {"title":"ZZTest lesson","youtubeUrl":"https://youtu.be/dQw4w9WgXcQ","transcript":[]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    /** Title chỉ toàn khoảng trắng: nếu lọt tới service thì trim() thành chuỗi rỗng. */
    @Test
    void upload_blankTitle_returns400() throws Exception {
        mockMvc.perform(uploadWithMeta("""
                {"title":"   ","youtubeUrl":"https://youtu.be/dQw4w9WgXcQ","level":"INTERMEDIATE","transcript":[]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void upload_validMeta_stillCreates() throws Exception {
        mockMvc.perform(uploadWithMeta("""
                {"title":"ZZTest lesson","youtubeUrl":"https://youtu.be/dQw4w9WgXcQ","level":"INTERMEDIATE","transcript":[]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("ZZTest lesson"));
    }
}
