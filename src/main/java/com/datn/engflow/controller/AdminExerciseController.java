package com.datn.engflow.controller;

import com.datn.engflow.model.dto.request.ExerciseRequest;
import com.datn.engflow.model.dto.response.ExerciseResponse;
import com.datn.engflow.service.ExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/exercises")
@RequiredArgsConstructor
/**
 * class AdminExerciseController.
 */
public class AdminExerciseController {

    private final ExerciseService exerciseService;

    /**
     * Liệt kê bài tập cho màn quản trị, có lọc và phân trang — CHỈ ADMIN.
     *
     * <p>Lưu ý hiệu năng (AGENTS.md): lọc {@code q} dùng LIKE {@code %kw%} trên bảng
     * {@code exercises} rất lớn nên chậm hơn hẳn bản không lọc; không có index nào cứu
     * leading-wildcard, đừng cố "tối ưu" bằng cách thêm index.</p>
     *
     * @param lessonId   lọc theo bài học, null = mọi bài
     * @param type       lọc theo loại bài tập, null = mọi loại
     * @param difficulty lọc theo độ khó, null = mọi mức
     * @param q          từ khoá tìm kiếm, null = không lọc
     * @param page       chỉ số trang, mặc định 0
     * @param size       số bài mỗi trang, mặc định 20 (kẹp [1, 100])
     * @return trang bài tập dạng {@link ExerciseResponse}, sắp theo orderIndex rồi id
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ExerciseResponse>> getAllExercises(
            @RequestParam(required = false) Long lessonId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(exerciseService.getAdminExercisePage(lessonId, type, difficulty, q,
                PageRequest.of(Math.max(page, 0), size, Sort.by("orderIndex").ascending().and(Sort.by("id")))));
    }

    /**
     * Lấy chi tiết một bài tập theo id — CHỈ ADMIN.
     *
     * @param id id bài tập
     * @return bài tập dạng {@link ExerciseResponse}
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu bài tập không tồn tại
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExerciseResponse> getExercise(@PathVariable Long id) {
        return ResponseEntity.ok(exerciseService.getExercise(id));
    }

    /**
     * Tạo bài tập mới — CHỈ ADMIN.
     *
     * @param request dữ liệu bài tập, đã validate
     * @return bài tập vừa tạo (HTTP 200)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExerciseResponse> createExercise(@Valid @RequestBody ExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.createExercise(request));
    }

    /**
     * Cập nhật bài tập theo id — CHỈ ADMIN.
     *
     * @param id      id bài tập cần sửa
     * @param request dữ liệu thay thế (không {@code @Valid} — giữ hành vi cũ)
     * @return bài tập sau cập nhật
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu bài tập không tồn tại
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExerciseResponse> updateExercise(@PathVariable Long id,
                                                            @RequestBody ExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.updateExercise(id, request));
    }

    /**
     * Xoá bài tập theo id — CHỈ ADMIN.
     *
     * @param id id bài tập cần xoá
     * @return map {@code {success:true, message:"..."}}
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu bài tập không tồn tại
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteExercise(@PathVariable Long id) {
        exerciseService.deleteExercise(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Exercise deleted successfully");
        return ResponseEntity.ok(resp);
    }
}
