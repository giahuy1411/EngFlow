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

@Service
@RequiredArgsConstructor
/**
 * class SpeakingPromptService.
 */
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

    public List<SpeakingPrompt> getAllPromptsForAdmin() {
        return repository.findAllByOrderByOrderIndexAsc();
    }

    public Page<SpeakingPrompt> getAllPromptsForAdmin(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return repository.findAll(pageable);
        }
        return repository.searchByKeyword(keyword.trim(), pageable);
    }

    public SpeakingPrompt getPrompt(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SpeakingPrompt", "id", id));
    }

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

    @Transactional
    public void deletePrompt(Long id) {
        SpeakingPrompt prompt = getPrompt(id);
        if (submissionRepository.existsByPromptId(id)) {
            throw new BadRequestException("Không thể xóa đề đã có bài nộp; hãy ẩn hoặc lưu trữ đề");
        }
        repository.delete(prompt);
    }

    public List<SpeakingPrompt> getAllPromptsAdmin() {
        return repository.findAll();
    }

    private Lesson resolveLesson(Long lessonId) {
        if (lessonId == null) {
            return null;
        }
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
    }

    private SpeakingPromptMode resolveMode(CreateSpeakingPromptRequest request) {
        return request.getMode() == null ? SpeakingPromptMode.FREE_SPEAKING : request.getMode();
    }

    private void validateMode(CreateSpeakingPromptRequest request, SpeakingPromptMode mode) {
        if (mode == SpeakingPromptMode.READ_ALOUD
                && (request.getReferenceText() == null || request.getReferenceText().isBlank())) {
            throw new BadRequestException("Bài READ_ALOUD bắt buộc có referenceText");
        }
    }
}
