package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.FlashcardReviewRequest;
import com.datn.engflow.service.FlashcardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * API flashcard: ôn thẻ, xem mức độ thành thạo, và ghi nhận ngày học.
 *
 * <p>Mọi endpoint đều lấy email người dùng qua {@code authentication.getName()} (vì
 * {@code UserPrincipal} dùng email làm định danh) và yêu cầu đăng nhập.</p>
 */
@RestController
@RequestMapping("/api/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;

    /**
     * Ghi nhận kết quả ôn một flashcard (thang đánh giá SM-2 trong {@link FlashcardReviewRequest}).
     *
     * <p><b>Lưu ý:</b> UI hiện tại <b>không</b> gọi endpoint này — màn hình luyện flashcard đã bỏ
     * bước gửi chất lượng SM-2 và chuyển sang {@link #recordStudyDay} để ghi ngày học (audit-v12
     * F153). Endpoint vẫn được giữ cho tương thích API.</p>
     *
     * @param request        dữ liệu ôn thẻ, đã qua Bean Validation
     * @param authentication ngữ cảnh xác thực
     * @return 200 rỗng khi thành công, 401 nếu chưa đăng nhập
     */
    @PostMapping("/review")
    public ResponseEntity<?> reviewFlashcard(@Valid @RequestBody FlashcardReviewRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        String email = authentication.getName();
        flashcardService.reviewFlashcard(request, email);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Mức độ thuộc (mastery level) của một từ đối với người dùng hiện tại.
     *
     * @param vocabularyId id từ vựng cần tra
     * @param authentication thông tin đăng nhập; null ⇒ 401
     * @return mức độ thuộc (số nguyên); 401 nếu chưa đăng nhập
     */
    @GetMapping("/status/{vocabularyId}")
    public ResponseEntity<Integer> getStatus(@PathVariable Long vocabularyId, Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        String email = authentication.getName();
        Integer masteryLevel = flashcardService.getStatus(vocabularyId, email);
        return ResponseEntity.ok(masteryLevel);
    }

    /**
     * audit-v12 F153: the drill records its study day here so reading flashcards still counts
     * toward the streak. It no longer sends an SM-2 quality, so {@code /review} is not called
     * by the UI — this endpoint replaces it as the streak source for flashcards.
     */
    @PostMapping("/study")
    public ResponseEntity<?> recordStudyDay(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        flashcardService.recordStudyDay(authentication.getName());
        return ResponseEntity.ok().build();
    }
}
