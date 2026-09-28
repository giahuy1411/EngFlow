package com.datn.engflow.controller.speaking;

import com.datn.engflow.model.dto.request.GradeSpeakingSubmissionRequest;
import com.datn.engflow.model.dto.response.SpeakingSubmissionResponse;
import com.datn.engflow.model.entity.SpeakingSubmissionStatus;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.SpeakingSubmission;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.SpeakingSubmissionService;
import com.datn.engflow.service.UserService;
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

/**
 * Cổng REST cho bài nộp luyện nói (và luyện nói theo video — hai nhóm path là cùng một bộ
 * handler, frontend dùng cả {@code speaking-submissions} lẫn {@code video-submissions}).
 *
 * <p>Tầng controller — {@link SpeakingSubmissionService} lo toàn bộ nghiệp vụ: lưu media,
 * chạy pipeline chấm tự động, chấm tay của admin. {@link UserService#hasPremiumAccess} là
 * nguồn sự thật duy nhất cho quyền premium, dùng ở cả đường nộp bài lẫn đường xem bài premium.
 */
@RestController
@RequiredArgsConstructor
public class SpeakingSubmissionController {

    private final SpeakingSubmissionService submissionService;
    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Nộp bài luyện nói kèm tệp media. Đường này là nơi duy nhất chặn premium cho người dùng thường.
     *
     * @param userPrincipal người học đã đăng nhập
     * @param promptId      id đề luyện tương ứng
     * @param file          tệp ghi âm/ghi hình
     * @return bài nộp vừa tạo
     */
    @PostMapping({"/api/v1/speaking-submissions/upload", "/api/v1/video-submissions/upload"})
    public ResponseEntity<SpeakingSubmissionResponse> uploadSubmission(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("promptId") Long promptId,
            @RequestParam("file") MultipartFile file) {
        requireAuthenticated(userPrincipal);
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!userService.hasPremiumAccess(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Premium membership required");
        }
        SpeakingSubmission submission = submissionService.uploadSubmission(file, promptId, user);
        return ResponseEntity.ok(toResponse(submission));
    }

    /**
     * Runs the automatic assessment pipeline for one of the caller's submissions.
     *
     * @param userPrincipal authenticated learner
     * @param id            submission id
     * @return assessed submission with transcript, rubric, and alignment details
     */
    @PostMapping({"/api/v1/speaking-submissions/{id}/assess", "/api/v1/video-submissions/{id}/assess"})
    public ResponseEntity<SpeakingSubmissionResponse> assessSubmission(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        requireAuthenticated(userPrincipal);
        SpeakingSubmission submission = submissionService.getSubmissionForViewer(
                id, userPrincipal.getId(), isAdmin(userPrincipal));
        SpeakingSubmission assessed = submissionService.assessSubmission(submission.getId());
        return ResponseEntity.ok(toResponse(assessed));
    }

    /**
     * Trang bài nộp toàn hệ thống cho quản trị, lọc theo trạng thái chấm.
     *
     * @param userPrincipal người gọi, phải có ROLE_ADMIN
     * @param status        trạng thái lọc, null nghĩa là tất cả
     * @param page          số trang 0-based
     * @param size          số bản ghi mỗi trang, bị kẹp vào 1..100
     * @return trang bài nộp, mới nhất trước
     */
    @GetMapping({"/api/v1/admin/speaking-submissions", "/api/v1/admin/video-submissions"})
    public ResponseEntity<Page<SpeakingSubmissionResponse>> getAllSubmissionsForAdmin(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) SpeakingSubmissionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAuthenticated(userPrincipal);
        if (!isAdmin(userPrincipal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        PageRequest pageable = submissionPageRequest(page, size);
        return ResponseEntity.ok(submissionService
                .getAllSubmissionsForAdmin(status, pageable)
                .map(this::toResponse));
    }

    /**
     * Admin chấm tay một bài nộp.
     *
     * @param userPrincipal người gọi, phải có ROLE_ADMIN
     * @param id            id bài nộp
     * @param request       điểm và nhận xét
     * @return bài nộp sau khi chấm
     */
    @PatchMapping({"/api/v1/admin/speaking-submissions/{id}/grade", "/api/v1/admin/video-submissions/{id}/grade"})
    public ResponseEntity<SpeakingSubmissionResponse> gradeSubmission(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody GradeSpeakingSubmissionRequest request) {
        requireAuthenticated(userPrincipal);
        if (!isAdmin(userPrincipal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        SpeakingSubmission submission = submissionService.gradeSubmission(id, userPrincipal.getId(), request);
        return ResponseEntity.ok(toResponse(submission));
    }

    /**
     * Trang bài nộp của chính người gọi, mới nhất trước.
     *
     * @param userPrincipal người học đã đăng nhập
     * @param page          số trang 0-based
     * @param size          số bản ghi mỗi trang, bị kẹp vào 1..100
     * @return trang bài nộp của người gọi
     */
    @GetMapping({"/api/v1/speaking-submissions", "/api/v1/video-submissions"})
    public ResponseEntity<Page<SpeakingSubmissionResponse>> getUserSubmissions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAuthenticated(userPrincipal);
        PageRequest pageable = submissionPageRequest(page, size);
        return ResponseEntity.ok(submissionService
                .getUserSubmissions(userPrincipal.getId(), pageable)
                .map(this::toResponse));
    }

    /**
     * Trang bài nộp của người gọi cho một đề luyện cụ thể.
     *
     * @param userPrincipal người học đã đăng nhập
     * @param promptId      id đề luyện
     * @param page          số trang 0-based
     * @param size          số bản ghi mỗi trang, bị kẹp vào 1..100
     * @return trang bài nộp thuộc đề đó
     */
    @GetMapping({"/api/v1/speaking-prompts/{promptId}/submissions", "/api/v1/video-prompts/{promptId}/submissions"})
    public ResponseEntity<Page<SpeakingSubmissionResponse>> getUserPromptSubmissions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long promptId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAuthenticated(userPrincipal);
        PageRequest pageable = submissionPageRequest(page, size);
        return ResponseEntity.ok(submissionService
                .getUserPromptSubmissions(userPrincipal.getId(), promptId, pageable)
                .map(this::toResponse));
    }

    /**
     * Chi tiết một bài nộp mà người gọi được phép xem (bài của chính họ, hoặc bất kỳ bài nào
     * nếu người gọi là admin).
     *
     * @param userPrincipal người đang đăng nhập
     * @param id            id bài nộp
     * @return chi tiết bài nộp
     */
    @GetMapping({"/api/v1/speaking-submissions/{id}", "/api/v1/video-submissions/{id}"})
    public ResponseEntity<SpeakingSubmissionResponse> getSubmission(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        requireAuthenticated(userPrincipal);
        SpeakingSubmission submission = submissionService.getSubmissionForViewer(
                id, userPrincipal.getId(), isAdmin(userPrincipal));
        return ResponseEntity.ok(toResponse(submission));
    }

    private SpeakingSubmissionResponse toResponse(SpeakingSubmission submission) {
        return SpeakingSubmissionResponse.from(submission, submissionService.createMediaReadUrl(submission));
    }

    /**
     * Chuẩn hoá tham số phân trang: kẹp {@code size} vào 1..100, {@code page} về không âm,
     * luôn sắp mới nhất trước.
     *
     * @param page số trang do client gửi
     * @param size số bản ghi mỗi trang do client gửi
     * @return PageRequest đã kẹp
     */
    private PageRequest submissionPageRequest(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "submittedAt"));
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
     * Kiểm tra principal có mang ROLE_ADMIN hay không.
     *
     * @param userPrincipal principal đã qua {@link #requireAuthenticated}
     * @return true nếu có ROLE_ADMIN
     */
    private boolean isAdmin(UserPrincipal userPrincipal) {
        return userPrincipal.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
