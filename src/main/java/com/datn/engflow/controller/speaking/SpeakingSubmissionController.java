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

@RestController
@RequiredArgsConstructor
/**
 * class SpeakingSubmissionController.
 */
public class SpeakingSubmissionController {

    private final SpeakingSubmissionService submissionService;
    private final UserRepository userRepository;
    private final UserService userService;

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

    private PageRequest submissionPageRequest(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "submittedAt"));
    }

    private void requireAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
    }

    private boolean isAdmin(UserPrincipal userPrincipal) {
        return userPrincipal.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
