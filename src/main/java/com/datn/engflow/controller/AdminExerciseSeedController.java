package com.datn.engflow.controller;
import com.datn.engflow.service.ExerciseSeedService;
import com.datn.engflow.service.ExerciseSeedService.SeedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint admin sinh bài tập hàng loạt từ nội dung HTML của lesson.
 *
 * <p>Đây là thao tác nặng và có thể phá dữ liệu ({@code force=true} xoá sạch bài tập cũ trước khi
 * seed lại), nên bắt buộc quyền ADMIN ở hai lớp: rule {@code /api/admin/**} trong {@code SecurityConfig}
 * và {@code @PreAuthorize("hasRole('ADMIN')")} ngay trên method — annotation cấp method chứ không
 * phải cấp class như các controller AI khác, nên nếu thêm method mới phải tự gắn lại quyền.</p>
 *
 * <p>Không nhầm với {@code AdminAiExerciseController} / {@code AdminAnswerBackfillController} —
 * hai controller kia dùng base {@code /api/admin/exercises/ai}, còn controller này dùng
 * {@code /api/admin/exercises} với method {@code /seed}; các path không chồng nhau.</p>
 */
@RestController
@RequestMapping("/api/admin/exercises")
@RequiredArgsConstructor
public class AdminExerciseSeedController {

    private final ExerciseSeedService exerciseSeedService;

    /**
     * Parse HTML của TẤT CẢ lesson và sinh bài tập từ đó.
     *
     * @param force nếu true, xoá toàn bộ bài tập hiện có trước rồi seed lại
     * @return kết quả seed (số lesson/bài tập đã xử lý) — xem {@link SeedResult}
     */
    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeedResult> seedAllExercises(
            @RequestParam(defaultValue = "false") boolean force) {
        SeedResult result = exerciseSeedService.seedAllLessons(force);
        return ResponseEntity.ok(result);
    }
}
