package com.datn.engflow.controller.video;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.model.dto.video.VideoDtos.GradeVideoAttemptRequest;
import com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine;
import com.datn.engflow.model.dto.video.VideoDtos.VideoAttemptResponse;
import com.datn.engflow.model.dto.video.VideoDtos.VideoLessonDetail;
import com.datn.engflow.model.dto.video.VideoDtos.VideoLessonRequest;
import com.datn.engflow.model.dto.video.VideoDtos.VideoLessonSummary;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.SubtitleParser;
import com.datn.engflow.service.VideoLessonService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
/**
 * class VideoLessonController — "Học tiếng Anh qua video" (mô hình Corodomo).
 */
public class VideoLessonController {

    private final VideoLessonService videoLessonService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------- public

    @GetMapping("/api/v1/video-lessons")
    public ResponseEntity<Page<VideoLessonSummary>> list(
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        LessonLevel parsed = null;
        if (level != null && !level.isBlank() && !"ALL".equalsIgnoreCase(level)) {
            try {
                parsed = LessonLevel.valueOf(level.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Trình độ không hợp lệ: " + level);
            }
        }
        return ResponseEntity.ok(videoLessonService.listPublished(parsed, pageRequest(page, size)));
    }

    @GetMapping("/api/v1/video-lessons/{id}")
    public ResponseEntity<VideoLessonDetail> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(videoLessonService.getDetail(id, userPrincipal == null ? null : userPrincipal.getId()));
    }

    // ------------------------------------------------------------- attempts

    @PostMapping("/api/v1/video-lessons/{id}/attempts")
    public ResponseEntity<VideoAttemptResponse> submitAttempt(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestParam("lineIndex") Integer lineIndex,
            @RequestParam("file") MultipartFile file) {
        requireAuthenticated(userPrincipal);
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(videoLessonService.submitAttempt(id, lineIndex, file, user));
    }

    @GetMapping("/api/v1/video-attempts")
    public ResponseEntity<Page<VideoAttemptResponse>> myAttempts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAuthenticated(userPrincipal);
        return ResponseEntity.ok(videoLessonService.listAttemptsForUser(
                userPrincipal.getId(), pageRequest(page, size)));
    }

    // ---------------------------------------------------------------- admin

    @GetMapping("/api/v1/admin/video-lessons")
    public ResponseEntity<Page<VideoLessonSummary>> listForAdmin(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        requireAdmin(userPrincipal);
        return ResponseEntity.ok(videoLessonService.listAll(pageRequest(page, size)));
    }

    @PostMapping("/api/v1/admin/video-lessons")
    public ResponseEntity<VideoLessonSummary> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody VideoLessonRequest request) {
        requireAdmin(userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED).body(videoLessonService.create(request));
    }

    /** Multipart create: transcript file (.srt/.vtt) or raw text field. */
    @PostMapping(value = "/api/v1/admin/video-lessons/upload", consumes = "multipart/form-data")
    public ResponseEntity<VideoLessonSummary> createWithTranscriptFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestPart("meta") VideoLessonRequest meta,
            @RequestPart(value = "transcriptFile", required = false) MultipartFile transcriptFile,
            @RequestParam(value = "transcriptText", required = false) String transcriptText) {
        requireAdmin(userPrincipal);
        VideoLessonRequest withTranscript = new VideoLessonRequest(
                meta.title(), meta.description(), meta.youtubeUrl(), meta.level(), meta.category(),
                parseTranscriptInput(transcriptFile, transcriptText, meta.transcript()),
                meta.isPublished());
        return ResponseEntity.status(HttpStatus.CREATED).body(videoLessonService.create(withTranscript));
    }

    @PutMapping("/api/v1/admin/video-lessons/{id}")
    public ResponseEntity<VideoLessonSummary> update(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody VideoLessonRequest request) {
        requireAdmin(userPrincipal);
        return ResponseEntity.ok(videoLessonService.update(id, request));
    }

    @DeleteMapping("/api/v1/admin/video-lessons/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        requireAdmin(userPrincipal);
        videoLessonService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa bài học video"));
    }

    @GetMapping("/api/v1/admin/video-attempts")
    public ResponseEntity<Page<VideoAttemptResponse>> attemptsForAdmin(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(userPrincipal);
        return ResponseEntity.ok(videoLessonService.listAttemptsForAdmin(status, pageRequest(page, size)));
    }

    @PatchMapping("/api/v1/admin/video-attempts/{id}/grade")
    public ResponseEntity<VideoAttemptResponse> grade(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody GradeVideoAttemptRequest request) {
        requireAdmin(userPrincipal);
        return ResponseEntity.ok(videoLessonService.grade(id, userPrincipal.getId(), request));
    }

    // -------------------------------------------------------------- helpers

    private List<TranscriptLine> parseTranscriptInput(MultipartFile file, String rawText,
                                                      List<TranscriptLine> fallback) {
        String raw = null;
        if (file != null && !file.isEmpty()) {
            try {
                raw = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception ex) {
                throw new BadRequestException("Không đọc được tệp transcript");
            }
        } else if (rawText != null && !rawText.isBlank()) {
            raw = rawText;
        }
        if (raw == null) {
            return fallback;
        }
        String trimmed = raw.strip();
        if (trimmed.startsWith("[")) {
            try {
                return objectMapper.readValue(trimmed, new TypeReference<List<TranscriptLine>>() {
                });
            } catch (Exception ex) {
                throw new BadRequestException("Transcript JSON không hợp lệ");
            }
        }
        return SubtitleParser.parse(raw).stream()
                .map(cue -> new TranscriptLine(cue.start(), cue.end(), cue.text(), null))
                .toList();
    }

    private PageRequest pageRequest(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by("id").ascending());
    }

    private void requireAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
    }

    private void requireAdmin(UserPrincipal userPrincipal) {
        requireAuthenticated(userPrincipal);
        boolean admin = userPrincipal.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
