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
import com.datn.engflow.service.ShadowingAiGradingService;
import com.datn.engflow.service.SubtitleParser;
import com.datn.engflow.service.SubtitleTranslationService;
import com.datn.engflow.service.YouTubeTranscriptService;
import com.datn.engflow.service.VideoLessonService;
import com.datn.engflow.model.entity.VideoAttempt;

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

/**
 * Cổng REST cho mô hình "học tiếng Anh qua video" (kiểu Corodomo): người học xem bài giảng,
 * luyện shadowing từng câu thoại rồi xem điểm; admin thì tạo/sửa bài và chấm attempt.
 *
 * <p>Tầng controller — chỉ điều phối, không giữ logic nghiệp vụ. Giao việc bài/attempt cho
 * {@link VideoLessonService}; chấm điểm AI cho {@link ShadowingAiGradingService}; lấy phụ đề
 * YouTube qua {@link YouTubeTranscriptService} rồi dịch sang tiếng Việt bằng
 * {@link SubtitleTranslationService}. Media trả về được ký bằng {@link MediaSigner} để
 * {@link com.datn.engflow.controller.MediaProxyController} chấp nhận.
 */
@RestController
@RequiredArgsConstructor
public class VideoLessonController {

    private final VideoLessonService videoLessonService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ShadowingAiGradingService shadowingAiGradingService;
    private final SubtitleTranslationService subtitleTranslationService;
    private final YouTubeTranscriptService youTubeTranscriptService;
    private final com.datn.engflow.security.MediaSigner mediaSigner;

    // ------------------------------------------------------------- public

    /**
     * Danh sách bài đã publish, lọc theo trình độ.
     *
     * @param level tên {@link LessonLevel}; null/rỗng/"ALL" nghĩa là không lọc
     * @param page  số trang 0-based
     * @param size  số bản ghi mỗi trang, bị chặn vào khoảng 1..100
     * @return trang kết quả
     * @throws BadRequestException khi {@code level} không khớp hằng số nào
     */
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

    /**
     * Chi tiết một bài kèm danh sách câu thoại và trạng thái đã làm của người gọi.
     *
     * @param id            id bài video
     * @param userPrincipal người gọi, null với khách chưa đăng nhập
     * @return chi tiết bài đã cắt theo quyền xem của người gọi
     */
    @GetMapping("/api/v1/video-lessons/{id}")
    public ResponseEntity<VideoLessonDetail> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        boolean isAdmin = userPrincipal != null && userPrincipal.getAuthorities() != null
                && userPrincipal.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return ResponseEntity.ok(videoLessonService.getDetail(id,
                userPrincipal == null ? null : userPrincipal.getId(), isAdmin));
    }

    // ------------------------------------------------------------- attempts

    /**
     * Nộp bản ghi shadowing của một câu thoại để chấm.
     *
     * @param userPrincipal người học đã đăng nhập
     * @param id            id bài video
     * @param lineIndex     thứ tự câu thoại trong transcript
     * @param file          tệp ghi âm/ghi hình do người học tải lên
     * @return attempt vừa tạo, mediaUrl đã ký chữ ký
     */
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

    /**
     * Lịch sử attempt của chính người gọi, mới nhất trước.
     *
     * @param userPrincipal người học đã đăng nhập
     * @param page          số trang 0-based
     * @param size          số bản ghi mỗi trang, bị chặn vào khoảng 1..100
     * @return trang attempt của người gọi
     */
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

    /**
     * Danh sách bài cho quản trị, gồm cả bài nháp (không chỉ bài đã publish).
     *
     * @param userPrincipal người gọi, phải có ROLE_ADMIN
     * @param page          số trang 0-based
     * @param size          số bản ghi mỗi trang, bị chặn vào khoảng 1..100
     * @return trang bài video
     */
    @GetMapping("/api/v1/admin/video-lessons")
    public ResponseEntity<Page<VideoLessonSummary>> listForAdmin(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        requireAdmin(userPrincipal);
        return ResponseEntity.ok(videoLessonService.listAll(pageRequest(page, size)));
    }

    /**
     * Tạo bài video mới từ JSON, transcript gửi kèm trong body dưới dạng danh sách dòng.
     *
     * @param userPrincipal người gọi, phải có ROLE_ADMIN
     * @param request       metadata bài kèm transcript
     * @return bài vừa tạo
     */
    @PostMapping("/api/v1/admin/video-lessons")
    public ResponseEntity<VideoLessonSummary> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody VideoLessonRequest request) {
        requireAdmin(userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED).body(videoLessonService.create(request));
    }

    /**
     * Multipart create: transcript file (.srt/.vtt) or raw text field.
     *
     * @param userPrincipal  người gọi, phải có ROLE_ADMIN
     * @param meta           metadata bài (title/level bắt buộc)
     * @param transcriptFile tệp phụ đề, ưu tiên hơn {@code transcriptText}
     * @param transcriptText transcript dạng text thô, dùng khi không có tệp
     * @return bài vừa tạo
     * @throws BadRequestException khi tệp transcript đọc lỗi hoặc JSON transcript sai cú pháp
     */
    @PostMapping(value = "/api/v1/admin/video-lessons/upload", consumes = "multipart/form-data")
    public ResponseEntity<VideoLessonSummary> createWithTranscriptFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            // @Valid chặn payload thiếu title/level chạy thẳng vào service
            // (request.title().trim() NPE → 500); đường JSON cũng validate như vậy.
            @Valid @RequestPart("meta") VideoLessonRequest meta,
            @RequestPart(value = "transcriptFile", required = false) MultipartFile transcriptFile,
            @RequestParam(value = "transcriptText", required = false) String transcriptText) {
        requireAdmin(userPrincipal);
        VideoLessonRequest withTranscript = new VideoLessonRequest(
                meta.title(), meta.description(), meta.youtubeUrl(), meta.level(), meta.category(),
                parseTranscriptInput(transcriptFile, transcriptText, meta.transcript()),
                meta.isPublished());
        return ResponseEntity.status(HttpStatus.CREATED).body(videoLessonService.create(withTranscript));
    }

    /**
     * Cập nhật bài video đang tồn tại.
     *
     * @param userPrincipal người gọi, phải có ROLE_ADMIN
     * @param id            id bài cần sửa
     * @param request       metadata mới
     * @return bài sau khi cập nhật
     */
    @PutMapping("/api/v1/admin/video-lessons/{id}")
    public ResponseEntity<VideoLessonSummary> update(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody VideoLessonRequest request) {
        requireAdmin(userPrincipal);
        return ResponseEntity.ok(videoLessonService.update(id, request));
    }

    /**
     * Xóa bài video cùng dữ liệu phụ thuộc.
     *
     * @param userPrincipal người gọi, phải có ROLE_ADMIN
     * @param id            id bài cần xóa
     * @return body thông báo xóa thành công
     */
    @DeleteMapping("/api/v1/admin/video-lessons/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        requireAdmin(userPrincipal);
        videoLessonService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa bài học video"));
    }

    /**
     * Danh sách attempt toàn hệ thống cho quản trị, lọc theo trạng thái.
     *
     * @param userPrincipal người gọi, phải có ROLE_ADMIN
     * @param status        trạng thái lọc, null nghĩa là tất cả
     * @param page          số trang 0-based
     * @param size          số bản ghi mỗi trang, bị chặn vào khoảng 1..100
     * @return trang attempt
     */
    @GetMapping("/api/v1/admin/video-attempts")
    public ResponseEntity<Page<VideoAttemptResponse>> attemptsForAdmin(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(userPrincipal);
        return ResponseEntity.ok(videoLessonService.listAttemptsForAdmin(status, pageRequest(page, size)));
    }

    /**
     * Admin chấm điểm tay cho một attempt.
     *
     * @param userPrincipal người gọi, phải có ROLE_ADMIN
     * @param id            id attempt
     * @param request       điểm và nhận xét
     * @return attempt sau khi chấm
     */
    @PatchMapping("/api/v1/admin/video-attempts/{id}/grade")
    public ResponseEntity<VideoAttemptResponse> grade(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody GradeVideoAttemptRequest request) {
        requireAdmin(userPrincipal);
        return ResponseEntity.ok(videoLessonService.grade(id, userPrincipal.getId(), request));
    }

    /**
     * AI grading: Whisper transcript → coverage score → LLM feedback.
     *
     * @param userPrincipal người gọi, phải có ROLE_ADMIN
     * @param id            id attempt cần chấm bằng AI
     * @return attempt sau khi chấm, mediaUrl đã ký
     */
    @PostMapping("/api/v1/admin/video-attempts/{id}/ai-grade")
    public ResponseEntity<VideoAttemptResponse> aiGrade(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        requireAdmin(userPrincipal);
        return ResponseEntity.ok(videoAttemptResponse(shadowingAiGradingService.aiGrade(id)));
    }

    /**
     * AI subtitle translation: fills textVi from textEn (fail-soft).
     *
     * @param userPrincipal người gọi, phải có ROLE_ADMIN
     * @param lines          các dòng phụ đề gốc tiếng Anh
     * @return các dòng sau khi điền bản dịch tiếng Việt
     */
    @PostMapping("/api/v1/admin/video-lessons/translate-transcript")
    public ResponseEntity<List<TranscriptLine>> translateTranscript(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody List<TranscriptLine> lines) {
        requireAdmin(userPrincipal);
        return ResponseEntity.ok(subtitleTranslationService.translate(lines));
    }

    /**
     * Fetches an existing YouTube transcript (auto or human captions) and has
     * the local LLM translate it to Vietnamese in one step. Also returns the
     * video title and description so the admin form can be pre-filled.
     *
     * @param userPrincipal người gọi, phải có ROLE_ADMIN
     * @param body          map chứa khoá {@code url} trỏ tới video YouTube
     * @return transcript đã dịch cùng tiêu đề và mô tả video
     */
    @PostMapping("/api/v1/admin/video-lessons/fetch-youtube")
    public ResponseEntity<YouTubeTranscriptService.YoutubeTranscriptResult> fetchYoutube(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> body) {
        requireAdmin(userPrincipal);
        return ResponseEntity.ok(youTubeTranscriptService.fetchAndTranslate(body.getOrDefault("url", "")));
    }

    // -------------------------------------------------------------- helpers

    /**
     * Chọn nguồn transcript theo thứ tự ưu tiên tệp → text thô → giá trị có sẵn, rồi chuẩn hoá
     * về {@code List<TranscriptLine>}: JSON bắt đầu bằng {@code [} thì parse thẳng, còn lại coi là
     * phụ đề SRT/VTT và chuyển qua {@link SubtitleParser}.
     *
     * @param file      tệp transcript tải lên, có thể null
     * @param rawText   transcript dạng text thô, có thể null
     * @param fallback  danh sách dòng đã có sẵn trong payload metadata
     * @return danh sách dòng phụ đề đã chuẩn hoá
     * @throws BadRequestException khi đọc tệp lỗi, hoặc transcript là JSON sai cú pháp
     */
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

    /**
     * Dựng DTO từ entity attempt. Trường media chỉ có mặt khi attempt đã lưu media; URL trả về
     * là đường dẫn tương đối kèm chữ ký hết hạn do {@link MediaSigner} cấp.
     *
     * @param attempt entity attempt đã lưu
     * @return DTO mà client đọc được
     */
    private VideoAttemptResponse videoAttemptResponse(VideoAttempt attempt) {
        // audit-v6 F28: relative media URL (see VideoLessonService.toResponse)
        // audit-v7 F55: signed (exp+sig) so the private proxy accepts it.
        String mediaUrl = attempt.getMediaObjectKey() == null ? null
                : "/api/v1/media/" + attempt.getMediaObjectKey() + "?" + mediaSigner.paramsForObject(attempt.getMediaObjectKey());
        return new VideoAttemptResponse(
                attempt.getId(),
                attempt.getVideoLesson().getId(),
                attempt.getLineIndex(),
                attempt.getStatus(),
                attempt.getScore() == null ? null : attempt.getScore().doubleValue(),
                attempt.getAdminFeedback(),
                mediaUrl,
                attempt.getSubmittedAt() == null ? null : attempt.getSubmittedAt().format(
                        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    /**
     * Chuẩn hoá tham số phân trang do client gửi: kẹp {@code size} vào 1..100 và {@code page} về
     * không âm, luôn sắp theo {@code id} tăng dần để thứ tự ổn định giữa các lần gọi.
     *
     * @param page số trang do client gửi
     * @param size số bản ghi mỗi trang do client gửi
     * @return PageRequest đã kẹp
     */
    private PageRequest pageRequest(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by("id").ascending());
    }

    /**
     * Chặn khách chưa đăng nhập.
     *
     * @param userPrincipal principal lấy từ {@code @AuthenticationPrincipal}
     * @throws ResponseStatusException với 401 khi principal null
     */
    private void requireAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
    }

    /**
     * Chặn người dùng đã đăng nhập nhưng không có ROLE_ADMIN.
     *
     * @param userPrincipal principal lấy từ {@code @AuthenticationPrincipal}
     * @throws ResponseStatusException 401 khi chưa đăng nhập, 403 khi không phải admin
     */
    private void requireAdmin(UserPrincipal userPrincipal) {
        requireAuthenticated(userPrincipal);
        boolean admin = userPrincipal.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
