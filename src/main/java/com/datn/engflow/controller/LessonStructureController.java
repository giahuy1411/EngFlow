package com.datn.engflow.controller;

import com.datn.engflow.security.SafeUploadNames;
import com.datn.engflow.service.CloudinaryService;
import com.datn.engflow.service.LessonService;
import com.datn.engflow.model.dto.request.BlockRequest;
import com.datn.engflow.model.dto.request.SectionRequest;
import com.datn.engflow.model.dto.response.SectionResponse;
import com.datn.engflow.service.LessonStructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
/**
 * class LessonStructureController.
 */
public class LessonStructureController {

    private final LessonStructureService lessonStructureService;
    private final CloudinaryService cloudinaryService;
    private final LessonService lessonService;

    @GetMapping("/api/admin/lessons/{lessonId}/structure")
    public ResponseEntity<List<SectionResponse>> getAdminStructure(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonStructureService.getLessonStructure(lessonId));
    }

    @PostMapping("/api/admin/lessons/{lessonId}/sections")
    public ResponseEntity<SectionResponse> addSection(@PathVariable Long lessonId, @RequestBody SectionRequest request) {
        return ResponseEntity.ok(lessonStructureService.addSection(lessonId, request));
    }

    @PutMapping("/api/admin/sections/{sectionId}")
    public ResponseEntity<SectionResponse> updateSection(@PathVariable Long sectionId, @RequestBody SectionRequest request) {
        return ResponseEntity.ok(lessonStructureService.updateSection(sectionId, request));
    }

    @DeleteMapping("/api/admin/sections/{sectionId}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long sectionId) {
        lessonStructureService.deleteSection(sectionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/sections/{sectionId}/blocks")
    public ResponseEntity<SectionResponse> addBlock(@PathVariable Long sectionId, @RequestBody BlockRequest request) {
        return ResponseEntity.ok(lessonStructureService.addBlock(sectionId, request));
    }

    @PutMapping("/api/admin/blocks/{blockId}")
    public ResponseEntity<SectionResponse> updateBlock(@PathVariable Long blockId, @RequestBody BlockRequest request) {
        return ResponseEntity.ok(lessonStructureService.updateBlock(blockId, request));
    }

    @DeleteMapping("/api/admin/blocks/{blockId}")
    public ResponseEntity<Void> deleteBlock(@PathVariable Long blockId) {
        lessonStructureService.deleteBlock(blockId);
        return ResponseEntity.noContent().build();
    }

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

    @GetMapping("/api/lessons/{lessonId}/structure")
    public ResponseEntity<List<SectionResponse>> getLessonStructure(
            @PathVariable Long lessonId,
            Authentication authentication) {
        // audit-v10 F126: bai nhap khong duoc doc cong khai (admin bo qua de preview).
        //
        // Day la duong DOC noi dung bai hoc — cung ho voi F88 (/exercises),
        // F105 (/grade, /submit) va F115 (/lesson-submissions). No bi bo sot
        // khi guard duoc ap tay tung controller, va do duoc la RO RI THAT:
        // lesson 10889 (is_published=0) tra 8.013 byte noi dung bai hoc that
        // cho student, trong khi GET /api/lessons/10889 tra 404.
        lessonService.assertLessonVisible(lessonId, isAdmin(authentication));
        return ResponseEntity.ok(lessonStructureService.getLessonStructure(lessonId));
    }

    /** True khi principal that su co ROLE_ADMIN (anonymous user khong bao gio co). */
    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String name = authentication.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return false;
        }
        return authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
