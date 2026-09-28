package com.datn.engflow.controller.payment;

import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Điểm vào cho luồng thanh toán Premium qua SePay.
 *
 * <p>Ba endpoint: webhook SePay (server-to-server, {@code permitAll} ở SecurityConfig vì SePay
 * không mang JWT — bảo vệ bằng chữ ký HMAC), tạo đơn hàng, và tra trạng thái Premium của chính
 * người gọi. Việc xác minh chữ ký và toàn bộ nghiệp vụ nằm ở {@link PaymentService}.</p>
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Nhận webhook xác nhận chuyển khoản từ SePay và kích hoạt Premium nếu hợp lệ.
     *
     * <p>Thân request được nhận dưới dạng <b>chuỗi thô</b> ({@code rawBody}) chứ không parse thành
     * object, vì chữ ký phải được tính trên đúng bytes gốc — parse rồi serialize lại sẽ làm lệch
     * chữ ký. Chữ ký nằm ở header {@code X-Signature} hoặc {@code X-Sepay-Signature} (SePay dùng
     * một trong hai tuỳ phiên bản); {@link PaymentService#processWebhook} xác minh HMAC trên chuỗi
     * {@code "<ts>.<rawBody>"} bằng {@code SEPAY_WEBHOOK_SECRET}.</p>
     *
     * <p><b>Chống replay:</b> timestamp {@code X-Sepay-Timestamp} phải nằm trong cửa sổ ±5 phút so
     * với đồng hồ server; chữ ký đúng nhưng timestamp quá cũ (hoặc quá xa tương lai) vẫn bị từ chối
     * với "Invalid signature". Nhờ vậy một request hợp lệ bị bắt lại không thể kích hoạt Premium
     * lần thứ hai.</p>
     *
     * @param rawBody        nguyên văn thân request — nguồn duy nhất để tính lại chữ ký
     * @param headerSignature chữ ký ở header {@code X-Signature} (có thể null)
     * @param sepaySignature  chữ ký ở header {@code X-Sepay-Signature} (có thể null)
     * @param sepayTimestamp  timestamp SePay gửi kèm, dùng cho cửa sổ chống replay
     * @return kết quả xử lý (thành công / bỏ qua / lý do từ chối) dưới dạng JSON
     */
    @PostMapping("/api/webhook/sepay")
    public ResponseEntity<Map<String, Object>> sepayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Signature", required = false) String headerSignature,
            @RequestHeader(value = "X-Sepay-Signature", required = false) String sepaySignature,
            @RequestHeader(value = "X-Sepay-Timestamp", required = false) String sepayTimestamp) {
        Map<String, Object> result = paymentService.processWebhook(
                rawBody, headerSignature, sepaySignature, sepayTimestamp);
        return ResponseEntity.ok(result);
    }

    /**
     * Tạo một đơn hàng Premium đang chờ thanh toán cho người dùng đang đăng nhập.
     *
     * <p>Trả về thông tin đơn (mã đơn dạng {@code ENG...}, số tiền, nội dung chuyển khoản, URL ảnh
     * QR SePay) để frontend dựng màn hình quét mã. {@code planType} lấy từ body, mặc định
     * {@code "MONTH"} khi client không gửi. Endpoint này nằm trong bucket rate-limit {@code :order}
     * (10/phút) vì mỗi lần gọi sinh một hàng trong {@code payment_transactions}.</p>
     *
     * @param userPrincipal người gọi, lấy từ JWT
     * @param body          payload tuỳ chọn, có thể chứa {@code planType}
     * @return thông tin đơn hàng, hoặc 401 nếu không có principal
     */
    @PostMapping("/api/v1/payment/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> body) {
        String planType = body.getOrDefault("planType", "MONTH");
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> result = paymentService.createOrder(userPrincipal.getId(), planType);
        return ResponseEntity.ok(result);
    }

    /**
     * Tra trạng thái Premium hiện tại của người dùng đang đăng nhập (còn hạn hay không, ngày hết
     * hạn nếu có). Frontend gọi endpoint này để quyết định hiển thị giao diện Premium.
     *
     * @param userPrincipal người gọi, lấy từ JWT
     * @return trạng thái Premium dưới dạng JSON, hoặc 401 nếu không có principal
     */
    @GetMapping("/api/v1/payment/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> status = paymentService.getPremiumStatus(userPrincipal.getId());
        return ResponseEntity.ok(status);
    }
}
