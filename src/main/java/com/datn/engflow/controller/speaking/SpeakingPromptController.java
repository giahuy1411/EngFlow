package com.datn.engflow.controller.speaking;

import com.datn.engflow.model.dto.request.CreateSpeakingPromptRequest;
import com.datn.engflow.model.dto.response.SpeakingPromptResponse;
import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.AiPromptService;
import com.datn.engflow.service.SpeakingPromptService;
import com.datn.engflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
/**
 * class SpeakingPromptController.
 */
public class SpeakingPromptController {

    private final SpeakingPromptService SpeakingPromptService;
    private final AiPromptService aiPromptService;
    private final UserService userService;

    /**
     * Liệt kê TẤT CẢ đề luyện nói cho màn quản trị, kể cả đề nháp/chưa publish.
     *
     * <p>Chỉ ADMIN gọi được — chặn 2 lớp: {@code SecurityConfig} cho {@code /api/v1/admin/**}
     * và {@code @PreAuthorize}. Khác endpoint public ở chỗ KHÔNG lọc theo premium: admin cần
     * thấy đủ để biên tập. Phơi cả hai alias {@code /api/v1/admin/speaking-prompts} và
     * {@code /api/v1/admin/video-prompts}.</p>
     *
     * @param q    từ khoá tìm kiếm, null = không lọc
     * @param page chỉ số trang, mặc định 0 (đã kẹp âm về 0)
     * @param size số bản ghi mỗi trang, mặc định 20 (kẹp trong [1, 100])
     * @return trang đề dạng {@link SpeakingPromptResponse}, sắp theo orderIndex rồi id
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping({"/api/v1/admin/speaking-prompts", "/api/v1/admin/video-prompts"})
    public ResponseEntity<Page<SpeakingPromptResponse>> getAllPromptsForAdmin(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = promptPageRequest(page, size);
        return ResponseEntity.ok(SpeakingPromptService.getAllPromptsForAdmin(q, pageable).map(SpeakingPromptResponse::from));
    }

    /**
     * Tìm kiếm đề luyện nói theo từ khoá — nhánh PUBLIC (khách vãng lai xem được).
     *
     * <p>Chỉ khớp khi request CÓ tham số {@code q} (Spring tách mapping theo {@code params}).
     * Đề premium chỉ hiện với người đã đăng nhập và còn hạn gói, quyết định qua
     * {@link #premiumViewer} (đọc {@link UserService#hasPremiumAccess}).</p>
     *
     * @param principal người dùng hiện tại, null nếu khách vãng lai
     * @param q         từ khoá tìm kiếm (bắt buộc ở nhánh này)
     * @param page      chỉ số trang, mặc định 0
     * @param size      số bản ghi mỗi trang, mặc định 20 (kẹp [1, 100])
     * @return trang đề khả kiến với người xem
     */
    @GetMapping(value = {"/api/v1/speaking-prompts", "/api/v1/video-prompts"}, params = "q")
    public ResponseEntity<Page<SpeakingPromptResponse>> getAllPrompts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = promptPageRequest(page, size);
        return ResponseEntity.ok(SpeakingPromptService.getAllPrompts(q, pageable, premiumViewer(principal))
                .map(SpeakingPromptResponse::from));
    }

    /**
     * Liệt kê đề luyện nói khi client KHÔNG truyền từ khoá — nhánh PUBLIC.
     *
     * <p>Chỉ khớp khi request không có tham số {@code q}; uỷ quyền cho
     * {@link #getAllPrompts(UserPrincipal, String, int, int)} với {@code q = null} nên cùng
     * quy tắc premium.</p>
     *
     * @param principal người dùng hiện tại, null nếu khách vãng lai
     * @param page      chỉ số trang, mặc định 0
     * @param size      số bản ghi mỗi trang, mặc định 20
     * @return trang đề khả kiến với người xem
     */
    @GetMapping(value = {"/api/v1/speaking-prompts", "/api/v1/video-prompts"}, params = "!q")
    public ResponseEntity<Page<SpeakingPromptResponse>> getAllPrompts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return getAllPrompts(principal, null, page, size);
    }

    /**
     * Lấy chi tiết một đề luyện nói theo id — PUBLIC, nhưng đề premium bị ẩn với người không
     * có quyền (lọc qua {@link #premiumViewer}).
     *
     * @param principal người dùng hiện tại, null nếu khách vãng lai
     * @param id        id đề
     * @return đề dạng {@link SpeakingPromptResponse}
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu không có đề với id này
     */
    @GetMapping({"/api/v1/speaking-prompts/{id}", "/api/v1/video-prompts/{id}"})
    public ResponseEntity<SpeakingPromptResponse> getPrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(SpeakingPromptResponse.from(
                SpeakingPromptService.getPromptForViewer(id, premiumViewer(principal))));
    }

    /**
     * Hai endpoint trên permitAll (khách vãng lai vẫn xem được danh sách), nên
     * principal có thể null. Quyền premium đọc từ {@link UserService#hasPremiumAccess}
     * — cùng nguồn sự thật với gate nộp bài. Thực thể đã được {@code CustomUserDetailsService}
     * nạp fresh từ DB ở chính request này (JWT stateless, không cache), nên không cần
     * gọi repository lần nữa; tài khoản hết hạn gói vẫn không thấy đề premium.
     *
     * @param principal người dùng đã đăng nhập, null với khách vãng lai
     * @return true nếu được xem đề premium
     */
    private boolean premiumViewer(UserPrincipal principal) {
        if (principal == null) {
            return false;
        }
        return userService.hasPremiumAccess(principal.getUser());
    }


    /**
     * Tạo đề luyện nói mới — chỉ ADMIN.
     *
     * @param request dữ liệu đề (tiêu đề, nội dung, level, referenceText...), đã validate
     * @return đề vừa tạo, HTTP 201 Created
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({"/api/v1/admin/speaking-prompts", "/api/v1/admin/video-prompts"})
    public ResponseEntity<SpeakingPromptResponse> createPrompt(
            @Valid @RequestBody CreateSpeakingPromptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SpeakingPromptResponse.from(SpeakingPromptService.createPrompt(request)));
    }

    /**
     * Cập nhật đề luyện nói theo id — chỉ ADMIN.
     *
     * @param id      id đề cần sửa
     * @param request dữ liệu thay thế, đã validate
     * @return đề sau cập nhật
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu không có đề với id này
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping({"/api/v1/admin/speaking-prompts/{id}", "/api/v1/admin/video-prompts/{id}"})
    public ResponseEntity<SpeakingPromptResponse> updatePrompt(
            @PathVariable Long id,
            @Valid @RequestBody CreateSpeakingPromptRequest request) {
        return ResponseEntity.ok(SpeakingPromptResponse.from(SpeakingPromptService.updatePrompt(id, request)));
    }

    /**
     * Xoá đề luyện nói theo id — chỉ ADMIN.
     *
     * @param id id đề cần xoá
     * @return map chứa thông báo xác nhận
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu không có đề với id này
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping({"/api/v1/admin/speaking-prompts/{id}", "/api/v1/admin/video-prompts/{id}"})
    public ResponseEntity<Map<String, String>> deletePrompt(@PathVariable Long id) {
        SpeakingPromptService.deletePrompt(id);
        return ResponseEntity.ok(Map.of("message", "Prompt deleted successfully"));
    }

    /**
     * Sinh nội dung đề luyện nói bằng AI từ chủ đề + level — chỉ ADMIN.
     *
     * <p>Gọi LLM (Ollama) kiểu blocking: timeout 60 s, block 65 s. Trả 502 nếu AI lỗi/không
     * phản hồi, 400 nếu thiếu chủ đề hoặc AI trả rỗng. KHÔNG ghi DB — chỉ trả bản nháp để
     * admin duyệt rồi mới tạo đề qua {@link #createPrompt}.</p>
     *
     * @param body map có {@code topic} (bắt buộc) và {@code level} (mặc định "B1")
     * @return map gồm {@code content}, {@code referenceText} và {@code topic}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({"/api/v1/admin/speaking-prompts/ai-generate", "/api/v1/admin/video-prompts/ai-generate"})
    public ResponseEntity<Map<String, String>> aiGeneratePrompt(
            @RequestBody Map<String, String> body) {
        String topic = body.getOrDefault("topic", "");
        if (topic.isBlank()) {
            // audit-v8: ai-generate-full already guards this; without the same check an
            // empty topic still reached Ollama and came back as invented filler.
            return ResponseEntity.badRequest().body(Map.of("error", "Cần nhập chủ đề"));
        }
        String level = body.getOrDefault("level", "B1");
        // Blocking call with timeout: a Mono return value triggers an async error
        // redispatch that clears the SecurityContext and masks real failures as 401.
        String result;
        try {
            result = aiPromptService.generateSpeakingPrompt(topic, level)
                    .timeout(java.time.Duration.ofSeconds(60))
                    .block(java.time.Duration.ofSeconds(65));
        } catch (Exception ex) {
            log.error("AI prompt generation failed for topic '{}': {}", topic, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "AI không phản hồi"));
        }
        if (result == null || result.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "AI không phản hồi"));
        }
        return ResponseEntity.ok(Map.of(
                "content", result,
                "referenceText", aiPromptService.extractReferenceText(result),
                "topic", topic));
    }

    /**
     * Full AI draft for the "tạo đề luyện nói bằng AI" button: fills
     * title/description/prompt/referenceText/level in one LLM call.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({"/api/v1/admin/speaking-prompts/ai-generate-full", "/api/v1/admin/video-prompts/ai-generate-full"})
    public ResponseEntity<Map<String, String>> aiGenerateFullPrompt(
            @RequestBody Map<String, String> body) {
        String topic = body.getOrDefault("topic", "");
        if (topic.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cần nhập chủ đề"));
        }
        try {
            return ResponseEntity.ok(aiPromptService.generateFullPrompt(
                    topic, body.get("level"), body.get("mode")));
        } catch (Exception ex) {
            log.error("AI full prompt generation failed for topic '{}': {}", topic, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "AI không phản hồi — thử lại sau"));
        }
    }

    private PageRequest promptPageRequest(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by("orderIndex").ascending().and(Sort.by("id")));
    }
}
