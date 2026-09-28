package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.response.LessonListItemResponse;
import com.datn.engflow.model.dto.response.LessonResponse;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
/**
 * class LessonController.
 */
public class LessonController {

    private final LessonService lessonService;

    /**
     * Liệt kê bài học đã publish (phân trang, lọc theo từ khoá và level) — PUBLIC.
     *
     * <p>Chỉ trả bài đã publish với người thường; admin (nhận ra qua {@code authentication})
     * vẫn đi qua đường này nhưng muốn xem bản nháp thì dùng {@link #getLessonDetails}. Khách
     * vãng lai có {@code authentication == null} và vẫn xem được.</p>
     *
     * @param authentication thông tin xác thực, null nếu ẩn danh
     * @param q              từ khoá tìm kiếm, null = không lọc
     * @param level          level bài học, null = mọi level
     * @param page           chỉ số trang, mặc định 0 (kẹp âm về 0)
     * @param size           số bài mỗi trang, mặc định 12 (kẹp [1, 100])
     * @return trang bài học dạng {@link LessonListItemResponse}, sắp theo orderIndex rồi id
     */
    @GetMapping
    public ResponseEntity<Page<LessonListItemResponse>> getAllLessons(
            Authentication authentication,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LessonLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        String email = authentication != null ? authentication.getName() : null;
        size = Math.min(Math.max(size, 1), 100);
        Page<LessonListItemResponse> lessons = lessonService.getPublishedLessonPage(
                email, q, level, PageRequest.of(Math.max(page, 0), size, Sort.by("orderIndex").ascending().and(Sort.by("id"))));
        return ResponseEntity.ok(lessons);
    }

    /**
     * Lấy chi tiết một bài học — PUBLIC, nhưng bản nháp chỉ lộ với ADMIN để preview.
     *
     * <p>Giữ nguyên comment {@code audit-v8 F88}: quyền admin được xác định bằng cách quét
     * authority {@code ROLE_ADMIN} của authentication rồi truyền cờ {@code isAdmin} xuống
     * service — người thường không thấy bài nháp qua đường này.</p>
     *
     * @param id             id bài học
     * @param authentication thông tin xác thực, null nếu ẩn danh
     * @return chi tiết bài học dạng {@link LessonResponse}
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu bài không tồn tại
     */
    @GetMapping("/{id}")
    public ResponseEntity<LessonResponse> getLessonDetails(@PathVariable Long id, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        // audit-v8 F88: admin duoc doc ban nhap qua endpoint public de preview.
        boolean isAdmin = authentication != null && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        LessonResponse lesson = lessonService.getLessonDetails(id, email, isAdmin);
        return ResponseEntity.ok(lesson);
    }

    /**
     * Tạo bài học mới — CHỈ ADMIN (chặn ở {@code SecurityConfig}: POST {@code /api/lessons/**}
     * yêu cầu {@code hasRole("ADMIN")}).
     *
     * <p>Controller còn kiểm {@code authentication == null} để trả 401 rõ ràng thay vì để lỗi
     * khác nổi lên; guard phân quyền thật nằm ở filter chain.</p>
     *
     * @param lessonRequest dữ liệu bài học, đã validate
     * @param authentication thông tin xác thực, null → 401
     * @return bài vừa tạo, HTTP 201 Created
     */
    @PostMapping
    public ResponseEntity<LessonResponse> createLesson(@Valid @RequestBody LessonRequest lessonRequest, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LessonResponse created = lessonService.createLesson(lessonRequest);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Cập nhật bài học theo id — CHỈ ADMIN (SecurityConfig chặn PUT {@code /api/lessons/**}).
     *
     * @param id             id bài học cần sửa
     * @param lessonRequest  dữ liệu thay thế, đã validate
     * @param authentication thông tin xác thực, null → 401
     * @return bài sau cập nhật
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu bài không tồn tại
     */
    @PutMapping("/{id}")
    public ResponseEntity<LessonResponse> updateLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest lessonRequest, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LessonResponse updated = lessonService.updateLesson(id, lessonRequest);
        return ResponseEntity.ok(updated);
    }

    /**
     * Xoá bài học theo id — CHỈ ADMIN (SecurityConfig chặn DELETE {@code /api/lessons/**}).
     *
     * @param id             id bài học cần xoá
     * @param authentication thông tin xác thực, null → 401
     * @return HTTP 204 No Content khi xoá thành công
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu bài không tồn tại
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }
}
