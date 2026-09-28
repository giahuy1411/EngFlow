package com.datn.engflow.controller;

import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.VocabularyRepository;
import com.datn.engflow.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.Cacheable;
import com.datn.engflow.service.DictionaryService;
import com.datn.engflow.service.VocabularyService;

import java.util.List;

/**
 * API từ vựng: danh sách, tìm kiếm nội bộ, proxy từ điển ngoài, và tạo từ mới.
 *
 * <p>Lưu ý phân quyền (xem {@code SecurityConfig}): chỉ {@code GET /api/vocabulary/search} và
 * {@code GET /api/vocabulary/dictionary/*} là {@code permitAll}; còn {@code GET /api/vocabulary}
 * (danh sách) rơi vào {@code anyRequest().authenticated()} nên <b>cần đăng nhập</b> — ẩn danh trả
 * 401 là đúng thiết kế. Các thao tác ghi ({@code POST}/{@code PUT}/{@code DELETE}) còn siết chặt hơn.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/vocabulary")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyRepository vocabularyRepository;
    private final DictionaryService dictionaryService;
    private final VocabularyService vocabularyService;

    /**
     * Liệt kê từ vựng có phân trang (mặc định 20 từ/trang, sắp theo {@code word}).
     *
     * <p>Đây <b>không</b> phải endpoint public: SecurityConfig chỉ mở {@code /search} và
     * {@code /dictionary/*}, nên request ẩn danh vào đây bị chặn 401.</p>
     *
     * @param pageable tham số phân trang/sắp xếp do Spring bind từ query string
     * @return một trang {@link Vocabulary}
     */
    @GetMapping
    public ResponseEntity<Page<Vocabulary>> list(@PageableDefault(size = 20, sort = "word") Pageable pageable) {
        return ResponseEntity.ok(vocabularyRepository.findAll(pageable));
    }

    /**
     * Tìm từ vựng theo chuỗi con (không phân biệt hoa thường) trong kho từ nội bộ.
     *
     * <p><b>Guard độ dài:</b> từ khoá dưới 2 ký tự (hoặc rỗng) trả ngay danh sách rỗng, không chạm
     * DB — tránh truy vấn LIKE quét toàn bảng với từ khoá quá ngắn vô nghĩa. Nhận cả hai tên tham
     * số {@code keyword} và {@code q}, ưu tiên {@code keyword} khi có.</p>
     *
     * <p>Endpoint này là public (permitAll) và vẫn được các công cụ sweep/perf dùng, nhưng
     * <b>không</b> còn nằm trên đường "tra từ" của frontend — đường đó đi thẳng tới proxy từ điển
     * {@code /dictionary/{word}} (xem {@code vocabularyService.js}).</p>
     *
     * @param keyword tham số ưu tiên
     * @param q       tham số thay thế khi {@code keyword} trống
     * @return danh sách từ khớp, hoặc rỗng nếu từ khoá quá ngắn
     */
    @GetMapping("/search")
    public ResponseEntity<List<Vocabulary>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String q) {
        String query = keyword.isBlank() ? q : keyword;
        if (query.isBlank() || query.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        // audit-v17 closing round: the `exact` fast path was removed together with the local
        // lookup fallback — the dictionary (proxy → direct) is now the ONLY lookup source.
        // This substring endpoint stays a public API (search-sort, deep-probe, perf-probe);
        // it is no longer consulted by the frontend's tra-từ flow.
        return ResponseEntity.ok(vocabularyRepository.findByWordContainingIgnoreCase(query));
    }

    /**
     * Proxy tra từ điển dictionaryapi.dev — browser ở VN đôi khi không kết nối
     * trực tiếp được tới API này, trong khi backend container thì được.
     * Cache + timeout nằm ở DictionaryService (bắt buộc tách class để
     * @Cacheable đi qua Spring proxy).
     * Fail-soft: lỗi upstream → "[]".
     */
    @GetMapping("/dictionary/{word}")
    public ResponseEntity<String> dictionaryProxy(@PathVariable String word) {
        String clean = word.replaceAll("[^a-zA-Z'-]", "").toLowerCase();
        if (clean.isBlank()) {
            return ResponseEntity.badRequest().body("[]");
        }
        return ResponseEntity.ok(dictionaryService.lookup(clean));
    }

    /**
     * Thêm một từ vựng mới vào kho bộ từ.
     *
     * <p>Quy tắc (audit-v12 F147): người dùng thường **bắt buộc** nêu {@code deckId} —
     * từ được gắn vào deck của chính họ ngay trong cùng transaction; admin được phép
     * thêm từ dùng chung không cần deck.
     *
     * @param request        dữ liệu từ vựng (đã validate)
     * @param deckId         deck sẽ chứa từ (bắt buộc với non-admin)
     * @param authentication thông tin đăng nhập; null ⇒ trả 401
     * @return từ vựng vừa tạo
     */
    @PostMapping
    public ResponseEntity<Vocabulary> create(
            @Valid @RequestBody VocabularyRequest request,
            @RequestParam(name = "deckId", required = false) Long deckId,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // audit-v12 F147: a non-admin must name the deck the word goes into, and the link is
        // made server-side in the same transaction. Previously the client had to make a
        // second call to link the deck, which could fail and strand the word in the shared
        // dictionary, and nothing checked that the deck belonged to the caller.
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        Long userId = (authentication.getPrincipal() instanceof UserPrincipal principal)
                ? principal.getId()
                : null;
        Vocabulary saved = vocabularyService.createScoped(request, deckId, userId, isAdmin);
        return ResponseEntity.ok(saved);
    }
}
