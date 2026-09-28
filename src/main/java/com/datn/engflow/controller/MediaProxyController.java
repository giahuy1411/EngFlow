package com.datn.engflow.controller;

import com.datn.engflow.repository.SpeakingSubmissionRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VideoAttemptRepository;
import com.datn.engflow.security.MediaSigner;
import com.datn.engflow.service.MinioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxy phục vụ media riêng tư từ MinIO qua đường dẫn {@code /api/v1/media/**}.
 *
 * <p>Rule trong {@code SecurityConfig} để {@code GET /api/v1/media/**} là {@code permitAll} — điều
 * này KHÔNG có nghĩa object nào cũng tải được. Quyền truy cập được quyết định ngay trong controller
 * bằng hai cách chứng minh (xem {@link #serveMedia}), nên vé HMAC mới là thứ bảo vệ thật, không phải
 * tầng filter. Lý do không dùng header {@code Authorization}: thẻ {@code <audio src>}/{@code <video src>}
 * của trình duyệt không gắn được header, phải ký trên URL.</p>
 *
 * <p>Nội dung trả về luôn kèm {@code Cache-Control: private, no-store} để proxy/CDN trung gian không
 * lưu bản ghi âm của người học.</p>
 */
@RestController
@RequiredArgsConstructor
public class MediaProxyController {

    private final MinioService minioService;
    private final MediaSigner mediaSigner;
    private final SpeakingSubmissionRepository submissionRepository;
    private final VideoAttemptRepository videoAttemptRepository;
    private final UserRepository userRepository;

    /**
     * Phục vụ một object media, chỉ khi caller chứng minh được quyền.
     *
     * <p><b>Hợp đồng bảo mật (audit-v7 F55):</b> bản ghi âm của người học là dữ liệu cá nhân. Trước
     * bản vá, một object key trần chính là URL tải công khai (IDOR). Nay chỉ chấp nhận một trong hai
     * bằng chứng:</p>
     * <ol>
     *   <li>vé hết hạn do server ký {@code ?exp=&sig=} — dạng DUY NHẤT mà UI sinh ra cho chủ sở hữu/admin,
     *       hoạt động được trong {@code <audio src>};</li>
     *   <li>URL kiểu cũ chưa có vé (lưu trước bản vá): chỉ cho qua khi principal đang đăng nhập SỞ HỮU
     *       object đó.</li>
     * </ol>
     * <p>Không đạt cả hai → 403.</p>
     *
     * @param request request hiện tại, dùng để lấy URI (suy ra object key) và tham số {@code exp}/{@code sig}
     * @return luồng nội dung kèm content-type theo đuôi file, 404 nếu path/key rỗng hoặc object không
     *         tồn tại, 403 nếu không chứng minh được quyền
     */
    @GetMapping("/api/v1/media/**")
    public ResponseEntity<InputStreamResource> serveMedia(HttpServletRequest request) {
        String path = request.getRequestURI();
        String prefix = "/api/v1/media/";
        if (!path.startsWith(prefix)) {
            return ResponseEntity.notFound().build();
        }
        String objectKey = path.substring(prefix.length()).replaceFirst("^/+", "");
        if (objectKey.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // audit-v7 F55: bản ghi âm của người học là dữ liệu cá nhân; trước đây một object key
        // trần chính là URL tải công khai (IDOR). Nay chỉ chấp nhận hai bằng chứng:
        //  1. vé hết hạn do server ký (?exp=&sig=) — dạng DUY NHẤT mà UI sinh cho chủ sở hữu/admin,
        //     hoạt động được trong <audio src>;
        //  2. URL kiểu cũ lưu trước bản vá: chỉ cho qua khi principal đang đăng nhập SỞ HỮU object.
        boolean signed = mediaSigner.verify(objectKey, request.getParameter("exp"), request.getParameter("sig"));
        if (!signed && !ownsCurrently(objectKey)) {
            return ResponseEntity.status(403).build();
        }
        return stream(objectKey);
    }

    /**
     * Kiểm tra principal hiện tại có sở hữu object hay không (đường dự phòng cho URL cũ không vé).
     *
     * <p>Phải loại cả {@link AnonymousAuthenticationToken} lẫn principal chưa xác thực: nếu chỉ hỏi
     * {@code isAuthenticated()} thì khách ẩn danh cũng trả true và đường dự phòng thành lỗ hổng.
     * Object hợp lệ khi thuộc một bài speaking HOẶC một video attempt của chính user đó.</p>
     *
     * @param objectKey key trong MinIO cần kiểm quyền sở hữu
     * @return true nếu user đang đăng nhập sở hữu object
     */
    private boolean ownsCurrently(String objectKey) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return userRepository.findByEmail(auth.getName())
                .map(u -> submissionRepository.existsByMediaObjectKeyAndUserId(objectKey, u.getId())
                        || videoAttemptRepository.existsByMediaObjectKeyAndUserId(objectKey, u.getId()))
                .orElse(false);
    }

    /**
     * Kéo object từ MinIO và dựng response, suy content-type từ đuôi file.
     *
     * <p>Đuôi lạ rơi về {@code application/octet-stream} (an toàn: không để trình duyệt đoán bừa).
     * {@code Content-Disposition: inline} để phát trực tiếp trong thẻ media; {@code no-store} để
     * không ai cache bản ghi âm; {@code Access-Control-Allow-Origin: *} phục vụ trình phát cross-origin.</p>
     *
     * @param objectKey key đã qua kiểm quyền
     * @return luồng nội dung, hoặc 404 nếu object không tồn tại trong MinIO
     */
    private ResponseEntity<InputStreamResource> stream(String objectKey) {
        InputStreamResource resource = minioService.getObject(objectKey);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        String contentType = "application/octet-stream";
        if (objectKey.endsWith(".webm")) {
            contentType = "audio/webm";
        } else if (objectKey.endsWith(".ogg")) {
            contentType = "audio/ogg";
        } else if (objectKey.endsWith(".mp3")) {
            contentType = "audio/mpeg";
        } else if (objectKey.endsWith(".wav")) {
            contentType = "audio/wav";
        } else if (objectKey.endsWith(".mp4")) {
            contentType = "video/mp4";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .body(resource);
    }
}
