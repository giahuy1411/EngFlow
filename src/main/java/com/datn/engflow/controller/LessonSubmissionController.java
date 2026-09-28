package com.datn.engflow.controller;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.LessonSubmissionRequest;
import com.datn.engflow.model.dto.response.LessonSubmissionDTO;
import com.datn.engflow.model.enums.SkillType;
import com.datn.engflow.service.LessonService;
import com.datn.engflow.service.LessonSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/lesson-submissions")
@RequiredArgsConstructor
/**
 * class LessonSubmissionController.
 */
public class LessonSubmissionController {

    private final LessonSubmissionService lessonSubmissionService;
    private final LessonService lessonService;

    /**
     * audit-v10 F115: bài nháp phải vô hình với người thường trên MỌI đường chạm
     * tới nó. F88 chặn đường đọc, F105 chặn đường chấm/nộp bài tập; đường nộp bài
     * kỹ năng (writing/speaking/reading) còn hở nên phải dùng cùng guard.
     */
    private static boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * Nộp bài theo kỹ năng (writing/speaking/reading) cho một bài học — yêu cầu đăng nhập.
     *
     * <p>Trước khi nộp phải qua {@code assertLessonVisible}: người thường KHÔNG được nộp cho
     * bài nháp (chỉ admin thấy bài nháp). Đây là mắt nối của chuỗi guard chống lộ bài nháp
     * (audit-v10 F115: F88 chặn đường đọc, F105 chặn đường nộp bài tập, F115 bịt nốt đường
     * nộp theo kỹ năng).</p>
     *
     * @param request        dữ liệu bài nộp, đã validate
     * @param authentication thông tin xác thực, null → 401
     * @return bài nộp dạng {@link LessonSubmissionDTO}
     * @throws com.datn.engflow.exception.ResourceNotFoundException nếu bài học không tồn tại hoặc không khả kiến
     */
    @PostMapping("/submit")
    public ResponseEntity<LessonSubmissionDTO> submitLessonSkill(@Valid @RequestBody LessonSubmissionRequest request,
                                                                 Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        lessonService.assertLessonVisible(request.getLessonId(), isAdmin(authentication));
        String email = authentication.getName();
        LessonSubmissionDTO dto = lessonSubmissionService.submitLessonSkill(request, email);
        return ResponseEntity.ok(dto);
    }

    /**
     * Tải file ghi âm của học viên lên — yêu cầu đăng nhập.
     *
     * <p>File rỗng bị từ chối 400 kèm thông báo tiếng Việt; lưu file do
     * {@link LessonSubmissionService#saveAudioFile} đảm nhiệm (đi qua allowlist tên tệp để
     * chặn stored XSS — xem {@code SafeUploadNames}). Response chỉ trả URL, không trả nội dung.</p>
     *
     * @param file           file ghi âm multipart
     * @param authentication thông tin xác thực, null → 401
     * @return map {@code {audioUrl}} hoặc 400 nếu file trống
     */
    @PostMapping("/upload-audio")
    public ResponseEntity<Map<String, String>> uploadAudio(@RequestParam("file") MultipartFile file,
                                                           Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        if (file.isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "File ghi âm trống.");
            return ResponseEntity.badRequest().body(err);
        }

        String audioUrl = lessonSubmissionService.saveAudioFile(file);
        Map<String, String> res = new HashMap<>();
        res.put("audioUrl", audioUrl);
        return ResponseEntity.ok(res);
    }

    /**
     * Lấy bài nộp của CHÍNH người dùng cho một bài học + kỹ năng — yêu cầu đăng nhập.
     *
     * <p>Cũng qua {@code assertLessonVisible} (audit-v10 F115) để bài nháp không lộ gián tiếp
     * qua bài đã nộp. Nếu chưa có bài nộp nào thì trả 200 kèm body null (không phải 404) để
     * client dễ xử lý trạng thái "chưa nộp".</p>
     *
     * @param lessonId       id bài học
     * @param skillType      kỹ năng (enum {@link SkillType})
     * @param authentication thông tin xác thực, null → 401
     * @return bài nộp của chính user, hoặc body null nếu chưa nộp
     */
    @GetMapping("/my/lesson/{lessonId}/skill/{skillType}")
    public ResponseEntity<LessonSubmissionDTO> getMySubmission(@PathVariable Long lessonId,
                                                               @PathVariable SkillType skillType,
                                                               Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // audit-v10 F115: cùng guard — bài nháp không được lộ qua bài đã nộp.
        lessonService.assertLessonVisible(lessonId, isAdmin(authentication));
        String email = authentication.getName();
        try {
            LessonSubmissionDTO dto = lessonSubmissionService.getLessonSkillSubmission(lessonId, skillType, email);
            return ResponseEntity.ok(dto);
        } catch (ResourceNotFoundException e) {
            // Return 200 OK with null if no submission exists yet
            return ResponseEntity.ok(null);
        }
    }
}
