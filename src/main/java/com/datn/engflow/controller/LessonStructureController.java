package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.BlockRequest;
import com.datn.engflow.model.dto.request.SectionRequest;
import com.datn.engflow.model.dto.response.SectionResponse;
import com.datn.engflow.service.LessonStructureService;
import lombok.RequiredArgsConstructor;
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
public class LessonStructureController {

    private final LessonStructureService lessonStructureService;

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

    @GetMapping("/api/lessons/{lessonId}/structure")
    public ResponseEntity<List<SectionResponse>> getLessonStructure(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonStructureService.getLessonStructure(lessonId));
    }
}
