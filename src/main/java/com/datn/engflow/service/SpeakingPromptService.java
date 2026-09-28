package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.CreateSpeakingPromptRequest;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.SpeakingPromptMode;
import com.datn.engflow.model.entity.SpeakingPrompt;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.SpeakingPromptRepository;
import com.datn.engflow.repository.SpeakingSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD and access control for speaking prompts — the scripts learners read or speak.
 *
 * <p>Layer: sits between {@code SpeakingPromptController} and the repositories. Public
 * reads are permission-gated on a {@code premiumViewer} flag supplied by the controller,
 * so a premium prompt is invisible (not merely hidden in the UI) to a caller without
 * access; {@link #getPromptForViewer} applies the same rule to a single row. Writes are
 * admin-only, enforced upstream by the controller's {@code @PreAuthorize}.</p>
 */
@Service
@RequiredArgsConstructor
public class SpeakingPromptService {
    private final SpeakingPromptRepository repository;
    private final LessonRepository lessonRepository;
    private final SpeakingSubmissionRepository submissionRepository;

    /**
     * Danh sách công khai, lọc theo quyền của người xem. {@code premiumViewer = false}
     * ẩn đề {@code isPremium = true}. Không có overload nào mặc định "xem hết":
     * caller phải khai báo quyền, kể cả khi chỉ thêm một endpoint trong tương lai.
     *
     * @param keyword       từ khóa tìm kiếm, null/blank là lấy toàn bộ đã publish
     * @param pageable      phân trang
     * @param premiumViewer người xem có quyền premium (hoặc admin)
     * @return trang đề nói mà người xem được phép thấy
     */
    public Page<SpeakingPrompt> getAllPrompts(String keyword, Pageable pageable, boolean premiumViewer) {
        if (keyword == null || keyword.isBlank()) {
            return premiumViewer
                    ? repository.findByIsPublishedTrue(pageable)
                    : repository.findByIsPremiumFalseAndIsPublishedTrue(pageable);
        }
        return repository.searchByKeyword(keyword.trim(), !premiumViewer, pageable);
    }

    /**
     * Đọc một đề nói ở đường công khai, chặn theo quyền.
     *
     * @param id            đề nói cần đọc
     * @param premiumViewer người xem có quyền premium (hoặc admin)
     * @return đề nói
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu không tồn tại
     * @throws AccessDeniedException                                nếu đề premium mà người xem chưa có quyền
     */
    public SpeakingPrompt getPromptForViewer(Long id, boolean premiumViewer) {
        SpeakingPrompt prompt = getPrompt(id);
        if (!premiumViewer && Boolean.TRUE.equals(prompt.getIsPremium())) {
            throw new AccessDeniedException("Đề luyện nói này dành cho thành viên Premium");
        }
        return prompt;
    }

    /**
     * Admin listing ordered by {@code orderIndex}, ignoring the publish and premium
     * filters the public path applies.
     *
     * @return every prompt, drafts and premium ones included
     */
    public List<SpeakingPrompt> getAllPromptsForAdmin() {
        return repository.findAllByOrderByOrderIndexAsc();
    }

    /**
     * Admin listing with optional keyword search; unlike the public path it shows
     * unpublished and premium prompts.
     *
     * @param keyword  từ khóa tìm kiếm, null/blank thì trả về tất cả
     * @param pageable phân trang
     * @return trang đề nói cho quản trị viên
     */
    public Page<SpeakingPrompt> getAllPromptsForAdmin(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return repository.findAll(pageable);
        }
        return repository.searchByKeyword(keyword.trim(), pageable);
    }

    /**
     * Loads a prompt by id without any permission check; callers that serve a
     * user-facing endpoint should go through {@link #getPromptForViewer} instead.
     *
     * @param id mã đề nói
     * @return đề nói
     * @throws ResourceNotFoundException nếu không tồn tại
     */
    public SpeakingPrompt getPrompt(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpeakingPrompt", "id", id));
    }

    /**
     * Creates a prompt, defaulting the optional fields the admin form may omit:
     * {@code maxDurationSeconds} to 120, {@code attemptLimit} to 10,
     * {@code isPremium} to false and {@code isPublished} to true.
     *
     * @param request dữ liệu đề nói từ form admin
     * @return đề nói đã lưu
     * @throws BadRequestException    nếu mode READ_ALOUD thiếu referenceText
     * @throws ResourceNotFoundException nếu lessonId không tồn tại
     */
    @Transactional
    public SpeakingPrompt createPrompt(CreateSpeakingPromptRequest request) {
        SpeakingPromptMode mode = resolveMode(request);
        validateMode(request, mode);
        SpeakingPrompt prompt = SpeakingPrompt.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .prompt(request.getPrompt())
                .lesson(resolveLesson(request.getLessonId()))
                .mode(mode)
                .referenceText(request.getReferenceText())
                .referenceMediaObjectKey(request.getReferenceMediaObjectKey())
                .referenceMediaUrl(request.getReferenceMediaUrl())
                .maxDurationSeconds(request.getMaxDurationSeconds() == null ? 120 : request.getMaxDurationSeconds())
                .attemptLimit(request.getAttemptLimit() == null ? 10 : request.getAttemptLimit())
                .level(request.getLevel())
                .category(request.getCategory())
                .isPremium(request.getIsPremium() != null ? request.getIsPremium() : false)
                .thumbnailUrl(request.getThumbnailUrl())
                .orderIndex(request.getOrderIndex())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : true)
                .build();
        return repository.save(prompt);
    }

    /**
     * Updates a prompt with "null means keep current" semantics for the fields the
     * trimmed admin form does not send (lesson, category, thumbnail, orderIndex,
     * reference media, duration and attempt limits).
     *
     * @param id      mã đề nói
     * @param request dữ liệu cập nhật
     * @return đề nói sau khi lưu
     * @throws ResourceNotFoundException nếu đề nói hoặc lessonId không tồn tại
     * @throws BadRequestException      nếu mode READ_ALOUD thiếu referenceText
     */
    @Transactional
    public SpeakingPrompt updatePrompt(Long id, CreateSpeakingPromptRequest request) {
        SpeakingPrompt prompt = getPrompt(id);
        SpeakingPromptMode mode = resolveMode(request);
        validateMode(request, mode);
        prompt.setTitle(request.getTitle());
        prompt.setDescription(request.getDescription());
        prompt.setPrompt(request.getPrompt());
        // audit-v6 F20: the trimmed admin form omits lesson/category/thumbnail/
        // orderIndex/referenceMedia — null in those fields means "keep current",
        // not "wipe". Without this guard, editing a prompt silently detached it
        // from its lesson and erased category/thumbnail/order (verified live:
        // PUT with only the 8 form fields nulled category/orderIndex/thumbnail).
        if (request.getLessonId() != null || prompt.getLesson() == null) {
            prompt.setLesson(resolveLesson(request.getLessonId()));
        }
        prompt.setMode(mode);
        prompt.setReferenceText(request.getReferenceText());
        if (request.getReferenceMediaObjectKey() != null) prompt.setReferenceMediaObjectKey(request.getReferenceMediaObjectKey());
        if (request.getReferenceMediaUrl() != null) prompt.setReferenceMediaUrl(request.getReferenceMediaUrl());
        prompt.setMaxDurationSeconds(request.getMaxDurationSeconds() == null
                ? prompt.getMaxDurationSeconds() : request.getMaxDurationSeconds());
        prompt.setAttemptLimit(request.getAttemptLimit() == null
                ? prompt.getAttemptLimit() : request.getAttemptLimit());
        prompt.setLevel(request.getLevel());
        if (request.getCategory() != null) prompt.setCategory(request.getCategory());
        prompt.setIsPremium(request.getIsPremium() != null ? request.getIsPremium() : prompt.getIsPremium());
        if (request.getThumbnailUrl() != null) prompt.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getOrderIndex() != null) prompt.setOrderIndex(request.getOrderIndex());
        prompt.setIsPublished(request.getIsPublished() != null ? request.getIsPublished() : prompt.getIsPublished());
        return repository.save(prompt);
    }

    /**
     * Deletes a prompt, refusing while any submission still references it so historical
     * attempts keep their target.
     *
     * @param id mã đề nói
     * @throws ResourceNotFoundException nếu đề nói không tồn tại
     * @throws BadRequestException      nếu đề đã có bài nộp
     */
    @Transactional
    public void deletePrompt(Long id) {
        SpeakingPrompt prompt = getPrompt(id);
        if (submissionRepository.existsByPromptId(id)) {
            throw new BadRequestException("Không thể xóa đề đã có bài nộp; hãy ẩn hoặc lưu trữ đề");
        }
        repository.delete(prompt);
    }

    /**
     * Unpaged, unsorted admin listing. Preferred by the console table; the paged
     * {@link #getAllPromptsForAdmin(String, Pageable)} is what the REST list endpoint uses.
     *
     * @return mọi đề nói, không sắp xếp
     */
    public List<SpeakingPrompt> getAllPromptsAdmin() {
        return repository.findAll();
    }

    /**
     * Resolves the optional lesson link of a prompt.
     *
     * @param lessonId mã lesson, null thì đề nói không gắn lesson
     * @return lesson tương ứng, hoặc null
     * @throws ResourceNotFoundException nếu lessonId không tồn tại
     */
    private Lesson resolveLesson(Long lessonId) {
        if (lessonId == null) {
            return null;
        }
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
    }

    /** Defaults an absent mode to {@link SpeakingPromptMode#FREE_SPEAKING}. */
    private SpeakingPromptMode resolveMode(CreateSpeakingPromptRequest request) {
        return request.getMode() == null ? SpeakingPromptMode.FREE_SPEAKING : request.getMode();
    }

    /**
     * Enforces the one mode-dependent rule: a READ_ALOUD task is scored against a script,
     * so a blank {@code referenceText} would make grading meaningless.
     *
     * @param request dữ liệu đề nói
     * @param mode    chế độ đã resolve
     * @throws BadRequestException nếu mode là READ_ALOUD mà referenceText trống
     */
    private void validateMode(CreateSpeakingPromptRequest request, SpeakingPromptMode mode) {
        if (mode == SpeakingPromptMode.READ_ALOUD
                && (request.getReferenceText() == null || request.getReferenceText().isBlank())) {
            throw new BadRequestException("Bài READ_ALOUD bắt buộc có referenceText");
        }
    }
}
