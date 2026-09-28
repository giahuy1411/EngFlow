package com.datn.engflow.controller;

import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.SrsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API luyện tập theo phương pháp lặp lại ngắt quãng (Spaced Repetition — SRS).
 *
 * <p>Mỗi lần người dùng tự đánh giá độ nhớ một từ ({@code quality} 0–5), {@link SrsService} tính
 * lại thời điểm đến hạn kế tiếp. Khoảng cách giữa các lần ôn giãn dần theo mức nhớ, nhưng bị
 * <b>chặn trên (interval cap)</b> để một từ "nhớ tốt" không bị đẩy xa tới mức vài tháng — giữ nhịp
 * ôn đều đặn cho người học.</p>
 */
@RestController
@RequestMapping("/api/srs")
@RequiredArgsConstructor
public class SrsController {

    private final SrsService srsService;

    /**
     * Ghi nhận kết quả ôn một từ và cập nhật lịch ôn kế tiếp.
     *
     * <p>Body cần {@code vocabId} và {@code quality}. {@code quality} phải trong khoảng 0–5; giá
     * trị ngoài khoảng trả 400 vì nó quyết định khoảng cách ôn (thang điểm SM-2). Việc ghi nhận
     * cũng tính vào hoạt động học trong ngày ({@code study_days}) nên được cộng vào chuỗi streak.</p>
     *
     * @param userPrincipal người ôn, lấy từ JWT
     * @param payload       map chứa {@code vocabId} và {@code quality}
     * @return thông báo ghi nhận thành công, 400 nếu thiếu/không hợp lệ, 401 nếu chưa đăng nhập
     */
    @PostMapping("/review")
    public ResponseEntity<?> reviewWord(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, Integer> payload) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Integer vocabIdObj = payload.get("vocabId");
        Integer qualityObj = payload.get("quality");
        if (vocabIdObj == null || qualityObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "vocabId và quality không được để trống"));
        }
        Long vocabId = vocabIdObj.longValue();
        int quality = qualityObj;
        if (quality < 0 || quality > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "quality phải từ 0 đến 5"));
        }
        srsService.reviewWord(userPrincipal.getId(), vocabId, quality);
        return ResponseEntity.ok(Map.of("message", "Review recorded successfully"));
    }

    /**
     * Lấy danh sách các từ trong một deck đã đến hạn ôn lại của người dùng hiện tại.
     *
     * <p>"Đến hạn" nghĩa là thời điểm ôn kế tiếp (đã tính từ lần đánh giá trước, có áp interval cap)
     * đã tới. Frontend dùng danh sách này để dựng phiên ôn.</p>
     *
     * @param userPrincipal người học, lấy từ JWT
     * @param deckId        deck cần lấy từ đến hạn
     * @return danh sách từ đến hạn, hoặc 401 nếu chưa đăng nhập
     */
    @GetMapping("/due/{deckId}")
    public ResponseEntity<?> getDueWords(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long deckId) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(srsService.getDueWords(userPrincipal.getId(), deckId));
    }

    /**
     * Thống kê ôn tập của người dùng hiện tại: số từ đến hạn, số từ đã thuộc,
     * tiến độ tổng thể trong các deck của họ.
     *
     * @param userPrincipal người học, lấy từ JWT (null ⇒ 401)
     * @return số liệu thống kê ôn tập; 401 nếu chưa đăng nhập
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStudyStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(srsService.getStudyStats(userPrincipal.getId()));
    }
}
