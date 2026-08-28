package com.datn.engflow.controller;

import com.datn.engflow.service.CloudinaryService;
import com.datn.engflow.model.dto.request.BlockRequest;
import com.datn.engflow.model.dto.request.SectionRequest;
import com.datn.engflow.model.dto.response.SectionResponse;
import com.datn.engflow.service.LessonStructureService;
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
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + ext;
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
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            org.springframework.core.io.Resource res =
                    new org.springframework.core.io.FileSystemResource(filePath.toFile());
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofDays(7)))
                    .body(res);
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
    public ResponseEntity<List<SectionResponse>> getLessonStructure(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonStructureService.getLessonStructure(lessonId));
    }
}
