package com.datn.engflow.controller;

import com.datn.engflow.security.SafeUploadNames;
import com.datn.engflow.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Admin media upload + first-party resource serving.
 *
 * <p>audit-v15: these three endpoints used to live in {@code LessonStructureController} (the
 * Lesson Builder "Đường B" controller). They are NOT part of that feature — they are shared
 * infrastructure used by other admin screens and by speaking submissions:
 *
 * <ul>
 *   <li>{@code POST /api/admin/upload} — exercise image/audio upload
 *       ({@code AdminExercises.vue}).</li>
 *   <li>{@code GET /api/resources/{filename}} — serves first-party files to any viewer;
 *       {@code LessonSubmissionService} builds speaking-media URLs against it.</li>
 *   <li>{@code POST /api/admin/audio-upload} — Cloudinary audio upload.</li>
 * </ul>
 *
 * <p>So when Lesson Builder was removed, these were extracted here rather than deleted.
 * Authorization is unchanged and enforced by {@code SecurityConfig} ({@code /api/admin/**}
 * requires ROLE_ADMIN; {@code GET /api/resources/**} is permitAll).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
/**
 * class AdminUploadController.
 */
public class AdminUploadController {

    private final CloudinaryService cloudinaryService;

    /**
     * Upload ảnh/audio của bài tập, lưu xuống đĩa và trả URL nội bộ — CHỈ ADMIN.
     *
     * <p><b>ĐÂY LÀ SURFACE BẢO MẬT.</b> File lưu vào {@code uploads/} và được phục vụ lại qua
     * {@code GET /api/resources/**} — route đó là {@code permitAll} và CÙNG ORIGIN với SPA, nên
     * một file mà trình duyệt render được ({@code .html}/{@code .svg}/{@code .js}) sẽ là stored
     * XSS đọc JWT trong localStorage. Vì vậy:</p>
     * <ul>
     *   <li>Tên ghi xuống đĩa đi qua {@link SafeUploadNames#extensionOf}: chỉ nhận extension
     *       nằm trong allowlist (ảnh/audio/video + text), tên file đổi thành UUID nên không
     *       ghi đè/không điều khiển đường dẫn.</li>
     *   <li>Khi phục vụ, {@link #getResource} mới là chỗ chốt content-type
     *       ({@link SafeUploadNames#contentTypeFor}) và ép tải về
     *       ({@link SafeUploadNames#forceDownload}).</li>
     * </ul>
     * <p>Thêm writer mới phải đi qua đúng hai hàm đó — xem {@code SafeUploadNames} và AGENTS.md
     * mục "Boundaries".</p>
     *
     * @param file file multipart cần lưu
     * @return map {@code {url}} trỏ tới {@code /api/resources/<uuid>.<ext>}
     */
    @PostMapping("/api/admin/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String ext = SafeUploadNames.extensionOf(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString() + "." + ext;
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path filePath = uploadDir.resolve(filename);
            Files.write(filePath, file.getBytes());
            String url = "/api/resources/" + filename;
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Phục vụ file first-party trong {@code uploads/} — {@code permitAll} (khách cũng tải được).
     *
     * <p><b>SURFACE BẢO MẬT — chốt chặn thứ hai của đường upload.</b> Hai lớp phòng thủ:</p>
     * <ol>
     *   <li><b>Path traversal:</b> tên bị từ chối nếu chứa {@code /} hoặc {@code ..} (sau khi
     *       chuẩn hoá {@code \} → {@code /}); đường dẫn còn được {@code normalize()} lại.</li>
     *   <li><b>Stored XSS:</b> content-type được GHIM theo extension allowlist
     *       ({@link SafeUploadNames#contentTypeFor}) chứ KHÔNG dò từ nội dung file; file không
     *       nằm trong allowlist ảnh/audio/video trả {@code application/octet-stream} và bị ép
     *       {@code Content-Disposition: attachment} ({@link SafeUploadNames#forceDownload}). Nhờ
     *       vậy một {@code .html}/{@code .svg}/{@code .js} đã lỡ nằm trên đĩa vẫn không thể
     *       render như document first-party (audit-v8 F81).</li>
     * </ol>
     *
     * @param filename tên file trong {@code uploads/} (đã chặn traversal ở trên)
     * @return file kèm content-type/Content-Disposition an toàn, 400 nếu tên không hợp lệ,
     *         404 nếu không tồn tại, 500 nếu lỗi đọc
     */
    @GetMapping("/api/resources/{filename:.+}")
    public ResponseEntity<Resource> getResource(@PathVariable String filename) {
        try {
            // Path traversal guard: reject names containing separators or dot-dot.
            String clean = filename.replace("\\", "/");
            if (clean.contains("/") || clean.contains("..")) {
                return ResponseEntity.badRequest().build();
            }
            Path filePath = Paths.get("uploads").resolve(clean).normalize();
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            // audit-v8 F81: pin the type from the allowlisted extension instead of
            // probing the file, so a .html/.svg/.js already on disk cannot be
            // rendered as a first-party document.
            String contentType = SafeUploadNames.contentTypeFor(clean);
            org.springframework.core.io.Resource res =
                    new org.springframework.core.io.FileSystemResource(filePath.toFile());
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofDays(7)));
            if (SafeUploadNames.forceDownload(clean)) {
                builder = builder.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment");
            }
            return builder.body(res);
        } catch (Exception e) {
            log.error("Resource fetch failed: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Upload audio lên Cloudinary và trả URL công khai — CHỈ ADMIN.
     *
     * <p>Đây là đường sinh listening audio của MCP Antigravity. Khác {@link #uploadFile}, file
     * KHÔNG lưu local mà đẩy sang Cloudinary; vì vậy không đi qua allowlist
     * {@code SafeUploadNames}. Lỗi upload trả 400 kèm message của exception.</p>
     *
     * @param file file audio multipart
     * @return map {@code {url}} là URL Cloudinary, hoặc 400 kèm {@code error} nếu upload lỗi
     */
    @PostMapping("/api/admin/audio-upload")
    public ResponseEntity<Map<String, String>> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            String url = cloudinaryService.uploadAudio(file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("Audio upload failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
