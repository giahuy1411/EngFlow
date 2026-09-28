package com.datn.engflow.controller;

import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API chuỗi ngày học (streak) và lịch sử hoạt động học.
 *
 * <p>Dữ liệu streak được suy ra từ bảng {@code study_days} qua {@code StudyActivityService}, chứ
 * không lưu một cột đếm cứng — nhờ vậy "hôm nay" luôn được tính theo múi giờ VN
 * ({@code Asia/Ho_Chi_Minh}) ở phía server, thống nhất với cách ghi hoạt động.</p>
 */
@RestController
@RequestMapping("/api/streak")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;
    private final com.datn.engflow.service.StudyActivityService studyActivityService;

    /**
     * Ảnh chụp (snapshot) hoạt động học trong 30 ngày gần nhất của người dùng: chuỗi ngày hiện tại,
     * chuỗi dài nhất, và dữ liệu lịch để vẽ {@code StreakCalendar}. Cửa sổ 30 ngày là cố định —
     * snapshot chỉ phục vụ hiển thị, không dùng cho tính toán streak ở nơi khác.
     *
     * @param userPrincipal người dùng, lấy từ JWT
     * @return snapshot 30 ngày, hoặc 401 nếu chưa đăng nhập
     */
    @GetMapping("/snapshot")
    public ResponseEntity<?> getSnapshot(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(studyActivityService.snapshot(userPrincipal.getId(), 30));
    }

    /**
     * Lịch sử ngày học trong {@code days} ngày gần nhất (mặc định 30).
     *
     * <p>Dùng cho lịch streak ở trang Profile: trả về danh sách ngày đã học để
     * giao diện tô sáng. Chỉ đọc, không ghi {@code study_days}.
     *
     * @param userPrincipal người dùng đang đăng nhập (null ⇒ trả 401)
     * @param days          số ngày nhìn lại, mặc định 30
     * @return danh sách ngày học; 401 nếu chưa đăng nhập
     */
    @GetMapping("/history")
    public ResponseEntity<?> getStreakHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "30") int days) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(streakService.getLoginDays(userPrincipal.getId(), days));
    }

    /**
     * Chuỗi hiện tại kèm "hôm nay" theo ngày server (ISO {@code yyyy-MM-dd}) để
     * lịch học trong Profile đóng khung ngày đúng múi giờ backend, thay vì để
     * trình duyệt tự suy ra từ đồng hồ máy khách.
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentStreak(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var snapshot = studyActivityService.snapshot(userPrincipal.getId(), 30);
        return ResponseEntity.ok(Map.of("currentStreak", snapshot.currentStreak(), "today", snapshot.today()));
    }
}
