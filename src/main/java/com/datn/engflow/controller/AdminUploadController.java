package com.datn.engflow.controller;

import com.datn.engflow.security.SafeUploadNames;
import com.datn.engflow.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Admin media upload + first-party resource serving.
 *
 * <p>audit-v15: these three endpoints used to live in {@code LessonStructureController} (the
 * Lesson Builder "Đường B" controller). They are NOT part of that feature — they are shared
 * infrastructure used by other admin screens and by speaking submissions:
 *
 * <ul>
 *   <li>{@code POST /api/admin/upload} — exercise image/audio upload
 *       ({@code AdminExercises.vue}).</li>
 *   <li>{@code GET /api/resources/{filename}} — serves first-party files to any viewer;
 *       {@code LessonSubmissionService} builds speaking-media URLs against it.</li>
 *   <li>{@code POST /api/admin/audio-upload} — Cloudinary audio upload.</li>
 * </ul>
 *
 * <p>So when Lesson Builder was removed, these were extracted here rather than deleted.
 * Authorization is unchanged and enforced by {@code SecurityConfig} ({@code /api/admin/**}
 * requires ROLE_ADMIN; {@code GET /api/resources/**} is permitAll).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
/**
 * class AdminUploadController.
 */
public class AdminUploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/api/admin/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String ext = SafeUploadNames.extensionOf(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString() + "." + ext;
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path filePath = uploadDir.resolve(filename);
            Files.write(filePath, file.getBytes());
            String url = "/api/resources/" + filename;
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/resources/{filename:.+}")
    public ResponseEntity<Resource> getResource(@PathVariable String filename) {
        try {
            // Path traversal guard: reject names containing separators or dot-dot.
            String clean = filename.replace("\\", "/");
            if (clean.contains("/") || clean.contains("..")) {
                return ResponseEntity.badRequest().build();
            }
            Path filePath = Paths.get("uploads").resolve(clean).normalize();
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            // audit-v8 F81: pin the type from the allowlisted extension instead of
            // probing the file, so a .html/.svg/.js already on disk cannot be
            // rendered as a first-party document.
            String contentType = SafeUploadNames.contentTypeFor(clean);
            org.springframework.core.io.Resource res =
                    new org.springframework.core.io.FileSystemResource(filePath.toFile());
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofDays(7)));
            if (SafeUploadNames.forceDownload(clean)) {
                builder = builder.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment");
            }
            return builder.body(res);
        } catch (Exception e) {
            log.error("Resource fetch failed: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/admin/audio-upload")
    public ResponseEntity<Map<String, String>> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            String url = cloudinaryService.uploadAudio(file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("Audio upload failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
