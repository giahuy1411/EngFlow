package com.datn.engflow.controller;

import com.datn.engflow.model.dto.response.SnapshotResponse;
import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.LessonSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
/**
 * class LessonSnapshotController.
 */
public class LessonSnapshotController {

    private final LessonSnapshotService snapshotService;

    @GetMapping("/api/admin/lessons/{lessonId}/snapshots")
    public ResponseEntity<List<SnapshotResponse>> getSnapshots(@PathVariable Long lessonId) {
        return ResponseEntity.ok(snapshotService.getSnapshots(lessonId));
    }

    @PostMapping("/api/admin/lessons/{lessonId}/snapshots")
    public ResponseEntity<Void> takeSnapshot(@PathVariable Long lessonId,
                                              @AuthenticationPrincipal UserPrincipal user) {
        snapshotService.takeSnapshot(lessonId, user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/admin/lessons/{lessonId}/snapshots/{snapshotId}/restore")
    public ResponseEntity<List<Map<String, Object>>> restoreSnapshot(@PathVariable Long lessonId,
                                                                      @PathVariable Long snapshotId) {
        return ResponseEntity.ok(snapshotService.restoreSnapshot(lessonId, snapshotId));
    }
}
