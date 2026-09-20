package com.datn.engflow.controller;

import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/streak")
@RequiredArgsConstructor
/**
 * class StreakController.
 */
public class StreakController {

    private final StreakService streakService;
    private final com.datn.engflow.service.StudyActivityService studyActivityService;

    @GetMapping("/snapshot")
    public ResponseEntity<?> getSnapshot(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(studyActivityService.snapshot(userPrincipal.getId(), 30));
    }

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
