package com.datn.engflow.controller;

import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.response.AdminStatsDTO;
import com.datn.engflow.model.dto.response.AdminUserDTO;
import com.datn.engflow.model.dto.response.LessonSummaryDTO;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

/**
 * Cổng REST cho màn hình quản trị: thống kê, quản lý tài khoản, bài học và từ vựng.
 *
 * <p>Tầng controller — chỉ chuyển tiếp xuống {@link AdminService}. Phần bài tập đã tách sang
 * {@link AdminExerciseController}. Việc chặn truy cập không nằm ở đây mà do
 * {@code SecurityConfig} áp ROLE_ADMIN cho toàn bộ {@code /api/admin/**}.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // (Exercises moved to AdminExerciseController)

    // --- Stats ---
    /**
     * Số liệu tổng quan cho dashboard quản trị.
     *
     * @return DTO thống kê
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // --- Users ---
    /**
     * Trang danh sách tài khoản, tìm theo tên/email nếu có {@code q}.
     *
     * @param q    từ khoá tìm kiếm
     * @param page số trang 0-based
     * @param size số bản ghi mỗi trang
     * @return trang DTO người dùng
     */
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDTO>> getAllUsers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(q, page, size));
    }

    /**
     * Bật/vô hiệu hoá tài khoản (không xóa dữ liệu học).
     *
     * @param id id người dùng
     * @return DTO người dùng ở trạng thái sau khi đảo
     */
    @PutMapping("/users/{id}/toggle-active")
    public ResponseEntity<AdminUserDTO> toggleUserActive(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserActive(id));
    }

    /**
     * Cấp/thu quyền admin cho một tài khoản.
     *
     * @param id id người dùng
     * @return DTO người dùng ở trạng thái sau khi đảo
     */
    @PutMapping("/users/{id}/toggle-admin")
    public ResponseEntity<AdminUserDTO> toggleUserAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserAdmin(id));
    }

    /**
     * Bật/vô hiệu hoá quyền premium thủ công.
     *
     * @param id id người dùng
     * @return DTO người dùng ở trạng thái sau khi đảo
     */
    @PutMapping("/users/{id}/toggle-premium")
    public ResponseEntity<AdminUserDTO> toggleUserPremium(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserPremium(id));
    }

    /**
     * Thu quyền premium (khác {@code toggle-premium} ở chỗ luôn ghi trạng thái tắt).
     *
     * @param id id người dùng
     * @return DTO người dùng sau khi thu quyền
     */
    @PutMapping("/users/{id}/revoke-premium")
    public ResponseEntity<AdminUserDTO> revokeUserPremium(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.revokeUserPremium(id));
    }

    // --- Lessons ---
    /**
     * Trang bài học cho quản trị, gồm cả bài nháp, lọc theo trình độ và từ khoá.
     *
     * @param q     từ khoá tìm theo tên bài
     * @param level trình độ lọc, null nghĩa là tất cả
     * @param page  số trang 0-based
     * @param size  số bản ghi mỗi trang
     * @return trang DTO tóm tắt bài học
     */
    @GetMapping("/lessons")
    public ResponseEntity<Page<LessonSummaryDTO>> getAllLessons(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LessonLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllLessonsAdmin(q, level, page, size));
    }

    /**
     * Chi tiết đầy đủ một bài học cho màn hình soạn thảo.
     *
     * @param id id bài học
     * @return entity bài học
     */
    @GetMapping("/lessons/{id}")
    public ResponseEntity<Lesson> getLesson(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getLesson(id));
    }

    /**
     * Tạo bài học mới.
     *
     * @param request dữ liệu bài học đã qua Bean Validation
     * @return bài học vừa tạo
     */
    @PostMapping("/lessons")
    public ResponseEntity<Lesson> createLesson(@Valid @RequestBody LessonRequest request) {
        return ResponseEntity.ok(adminService.createLesson(request));
    }

    /**
     * Cập nhật bài học đang tồn tại.
     *
     * @param id      id bài học
     * @param request dữ liệu bài học mới
     * @return bài học sau khi cập nhật
     */
    @PutMapping("/lessons/{id}")
    public ResponseEntity<Lesson> updateLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        return ResponseEntity.ok(adminService.updateLesson(id, request));
    }

    /**
     * Xóa bài học cùng dữ liệu phụ thuộc.
     *
     * @param id id bài học
     * @return 204 No Content
     */
    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        adminService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bật/vô hiệu hoá publish của bài học.
     *
     * @param id id bài học
     * @return bài học ở trạng thái sau khi đảo
     */
    @PutMapping("/lessons/{id}/toggle-publish")
    public ResponseEntity<Lesson> toggleLessonPublish(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleLessonPublish(id));
    }

    // --- Vocabulary ---
    /**
     * Trang từ vựng trong bảng dùng chung.
     *
     * @param page số trang 0-based
     * @param size số bản ghi mỗi trang
     * @return trang entity từ vựng
     */
    @GetMapping("/vocabulary")
    public ResponseEntity<Page<Vocabulary>> getAllVocabulary(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllVocabulary(page, size));
    }

    /**
     * Tạo từ vựng mới.
     *
     * @param request dữ liệu từ vựng đã qua Bean Validation
     * @return từ vựng vừa tạo
     */
    @PostMapping("/vocabulary")
    public ResponseEntity<Vocabulary> createVocabulary(@Valid @RequestBody VocabularyRequest request) {
        return ResponseEntity.ok(adminService.createVocabulary(request));
    }

    /**
     * Cập nhật từ vựng đang tồn tại.
     *
     * @param id      id từ vựng
     * @param request dữ liệu từ vựng mới
     * @return từ vựng sau khi cập nhật
     */
    @PutMapping("/vocabulary/{id}")
    public ResponseEntity<Vocabulary> updateVocabulary(@PathVariable Long id, @Valid @RequestBody VocabularyRequest request) {
        return ResponseEntity.ok(adminService.updateVocabulary(id, request));
    }

    /**
     * Xóa từ vựng.
     *
     * @param id id từ vựng
     * @return 204 No Content
     */
    @DeleteMapping("/vocabulary/{id}")
    public ResponseEntity<Void> deleteVocabulary(@PathVariable Long id) {
        adminService.deleteVocabulary(id);
        return ResponseEntity.noContent().build();
    }
}
