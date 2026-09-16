package com.datn.engflow.controller;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.service.LessonSubmissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * audit-v8 F81 — stored XSS through the upload / resource pair.
 *
 * <p>{@code GET /api/resources/{filename}} is permitAll and, behind the Vite (or any
 * same-origin reverse) proxy, is served from the SAME ORIGIN as the SPA. Both writers
 * used to keep the client-supplied extension, so a logged-in learner could POST
 * {@code lesson.html} to {@code /api/lesson-submissions/upload-audio} and open the
 * returned URL to run attacker JavaScript in the app origin and read the JWT out of
 * localStorage. Reproduced end to end in a real browser before this guard existed.
 *
 * <p>Guards under test: browser-active extensions are refused at write time, the
 * media type of a stored file is pinned from a fixed table at read time (never
 * probed), and anything that is not displayable media is pushed to a download.
 */
@SpringBootTest
@Transactional
class AuditV8UploadXssTest {

    private static final Path UPLOAD_DIR = Paths.get("uploads");

    @Autowired private WebApplicationContext wac;
    @Autowired private LessonSubmissionService lessonSubmissionService;

    private MockMvc mockMvc;
    private final List<Path> created = new ArrayList<>();

    @AfterEach void cleanup() throws Exception {
        mockMvc = null;
        for (Path p : created) {
            Files.deleteIfExists(p);
        }
        created.clear();
    }

    private MockMvc mvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        }
        return mockMvc;
    }

    private MockMultipartFile file(String name, String contentType) {
        return new MockMultipartFile("file", name, contentType, "<h1>payload</h1>".getBytes());
    }

    private Path writeOnDisk(String filename, byte[] content) throws Exception {
        Files.createDirectories(UPLOAD_DIR);
        Path p = UPLOAD_DIR.resolve(filename);
        Files.write(p, content);
        created.add(p);
        return p;
    }

    // ---- write time ----------------------------------------------------------

    @Test
    void adminUploaderRefusesHtmlSvgAndJs() throws Exception {
        for (String bad : List.of("lesson.html", "icon.svg", "evil.js", "page.xhtml")) {
            mvc().perform(multipart("/api/admin/upload").file(file(bad, "text/html"))
                    .with(user("admin@test.com").roles("ADMIN")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void learnerCannotReachAdminUploader() throws Exception {
        mvc().perform(multipart("/api/admin/upload").file(file("lesson.png", "image/png"))
                .with(user("student@test.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUploaderStillAcceptsRealMedia() throws Exception {
        String body = mvc().perform(multipart("/api/admin/upload").file(file("diagram.png", "image/png"))
                .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String url = body.replaceAll(".*\"url\"\\s*:\\s*\"", "").replaceAll("\".*", "");
        assertThat(url).matches("/api/resources/[0-9a-f-]+\\.png");
        created.add(UPLOAD_DIR.resolve(url.substring(url.lastIndexOf('/') + 1)));
    }

    @Test
    void learnerAudioUploadCannotCarryAnActiveExtension() {
        assertThatThrownBy(() -> lessonSubmissionService.saveAudioFile(file("lesson.html", "text/html")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> lessonSubmissionService.saveAudioFile(file("x.svg", "image/svg+xml")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void learnerAudioUploadWithoutAFilenameDefaultsToWebm() throws Exception {
        MockMultipartFile unnamed = new MockMultipartFile("file", "", "audio/webm", new byte[] {1, 2, 3});
        String url = lessonSubmissionService.saveAudioFile(unnamed);
        assertThat(url).endsWith(".webm");
        created.add(UPLOAD_DIR.resolve(url.substring(url.lastIndexOf('/') + 1)));
    }

    // ---- read time ----------------------------------------------------------

    @Test
    void guestGetsPlainTextAsOpaqueDownload() throws Exception {
        String name = "zz-v8-note.txt";
        writeOnDisk(name, "hello".getBytes());
        mvc().perform(get("/api/resources/" + name))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/octet-stream"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment"));
    }

    /** The file that made the attack work: already on disk, must not be rendered. */
    @Test
    void legacyHtmlOnDiskIsNeverServedAsHtml() throws Exception {
        String name = "zz-v8-legacy.html";
        writeOnDisk(name, "<script>stealToken()</script>".getBytes());
        String ct = mvc().perform(get("/api/resources/" + name))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment"))
                .andReturn().getResponse().getHeader(HttpHeaders.CONTENT_TYPE);
        assertThat(ct).doesNotContain("html").doesNotContain("javascript").doesNotContain("svg");
    }

    @Test
    void displayableMediaKeepsItsTypeAndStaysInline() throws Exception {
        String name = "zz-v8-audio.webm";
        writeOnDisk(name, new byte[] {1, 2, 3, 4});
        mvc().perform(get("/api/resources/" + name))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "video/webm"));
        assertThat(UPLOAD_DIR.resolve(name)).exists();
    }

    @Test
    void traversalStillRejected() throws Exception {
        mvc().perform(get("/api/resources/..%2F..%2Fpom.xml")).andExpect(status().isBadRequest());
    }
}
