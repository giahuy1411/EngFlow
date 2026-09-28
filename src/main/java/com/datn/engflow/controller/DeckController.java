package com.datn.engflow.controller;

import com.datn.engflow.model.dto.DeckRequest;
import com.datn.engflow.model.entity.Deck;
import com.datn.engflow.security.UserPrincipal;
import jakarta.validation.Valid;
import com.datn.engflow.service.DeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Cổng REST cho bộ từ vựng (deck) và các từ trong đó.
 *
 * <p>Tầng controller — chuyển tiếp xuống {@link DeckService}, là nơi kiểm tra sở hữu deck
 * và ghi từ vào deck. Cả nhóm {@code /api/decks/**} được {@code SecurityConfig} mở
 * {@code permitAll}, nên mọi endpoint đọc phải tự xử lý principal null.
 */
@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;

    /**
     * Trang bộ từ công khai, tìm theo tên nếu có {@code q}.
     *
     * @param q    từ khoá lọc theo tên deck, null hoặc rỗng nghĩa là không lọc
     * @param page số trang 0-based
     * @param size số bản ghi mỗi trang, bị kẹp vào 1..100
     * @return trang deck sắp theo tên rồi id
     */
    @GetMapping
    public ResponseEntity<?> getAllPublicDecks(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        size = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(deckService.getPublicDeckPage(q,
                PageRequest.of(Math.max(page, 0), size, Sort.by("name").ascending().and(Sort.by("id")))));
    }

    /**
     * Trang bộ từ của chính người gọi.
     *
     * @param userPrincipal người đang đăng nhập
     * @param q             từ khoá lọc theo tên deck
     * @param page          số trang 0-based
     * @param size          số bản ghi mỗi trang, bị kẹp vào 1..100
     * @return trang deck sở hữu bởi người gọi, hoặc 401 nếu chưa đăng nhập
     */
    @GetMapping("/my")
    public ResponseEntity<?> getUserDecks(@AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        // audit-v7 F34: GET /api/decks/** là permitAll nên principal có thể null —
        // trước đây userPrincipal.getId() NPE -> 500. Trả 401 đúng hợp đồng auth.
        if (userPrincipal == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập"));
        }
        size = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(deckService.getUserDeckPage(userPrincipal.getId(), q,
                PageRequest.of(Math.max(page, 0), size, Sort.by("name").ascending().and(Sort.by("id")))));
    }

    /**
     * Chi tiết một deck kèm danh sách từ bên trong.
     *
     * @param id            id deck
     * @param userPrincipal người gọi, null với khách chưa đăng nhập
     * @return deck đã cắt theo quyền xem của người gọi
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDeckById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        return ResponseEntity.ok(deckService.getDeckById(id, userId));
    }

    /**
     * Tạo deck mới thuộc sở hữu của người gọi.
     *
     * @param userPrincipal chủ sở hữu của deck mới
     * @param request       tên/mô tả deck đã qua Bean Validation
     * @return deck vừa tạo
     */
    @PostMapping
    public ResponseEntity<?> createDeck(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody DeckRequest request) {
        return ResponseEntity.ok(deckService.createDeck(request, userPrincipal.getId()));
    }

    /**
     * Đổi tên/mô tả một deck mà người gọi sở hữu.
     *
     * @param userPrincipal người gọi, dùng để kiểm tra sở hữu
     * @param id            id deck cần sửa
     * @param request       dữ liệu deck mới
     * @return deck sau khi cập nhật
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDeck(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody DeckRequest request) {
        return ResponseEntity.ok(deckService.updateDeck(id, request, userPrincipal.getId()));
    }

    /**
     * Xóa deck mà người gọi sở hữu.
     *
     * @param userPrincipal người gọi, dùng để kiểm tra sở hữu
     * @param id            id deck cần xóa
     * @return body thông báo xóa thành công
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDeck(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        deckService.deleteDeck(id, userPrincipal.getId());
        return ResponseEntity.ok(Map.of("message", "Deck deleted successfully"));
    }

    /**
     * Gắn một từ vào deck. Nhận cả hai khoá {@code vocabId} và {@code vocabularyId} vì
     * client cũ và mới dùng tên khác nhau.
     *
     * @param userPrincipal người gọi, dùng để kiểm tra sở hữu deck
     * @param id            id deck đích
     * @param payload       body chứa {@code vocabId} hoặc {@code vocabularyId}
     * @return body thông báo thành công, hoặc 400 nếu thiếu khoá id từ vựng
     */
    @PostMapping("/{id}/words")
    public ResponseEntity<?> addWordToDeck(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody Map<String, Long> payload) {
        Long vocabId = payload.get("vocabId") != null
                ? payload.get("vocabId")
                : payload.get("vocabularyId");
        if (vocabId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "vocabId is required"));
        }
        deckService.addWordToDeck(id, vocabId, userPrincipal.getId());
        return ResponseEntity.ok(Map.of("message", "Word added to deck successfully"));
    }
}
