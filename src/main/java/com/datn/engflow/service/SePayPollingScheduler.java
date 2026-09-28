package com.datn.engflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Đường polling dự phòng định kỳ cho SePay. Webhook mới là kênh xác nhận thanh
 * toán chính, nhưng khi phát triển cục bộ nó phụ thuộc vào một tunnel công khai
 * (Tailscale Funnel / ngrok) có thể đang tắt, cấu hình sai, hoặc không tới được
 * từ phía SePay. Scheduler này biến việc gọi API thành phương án dự phòng thật:
 * nó tự quét các đơn PENDING gần đây, nên một giao dịch được kích hoạt trong
 * khoảng ~1 phút kể cả khi người dùng không mở lại trang premium.
 *
 * <p>Chốt chi phí: mỗi lượt quét gọi SePay User API nhiều nhất một lần cho mỗi
 * đơn đang chờ (có trần), nằm gọn trong giới hạn rate limit đã tài liệu hoá. Đơn
 * cũ hơn cửa sổ lookback bị bỏ qua — coi như đã bị bỏ rơi.</p>
 *
 * <p>Lưu ý: {@code SePayApiService} dùng URL cứng {@code TRANSACTIONS_LIST_URL}
 * (biến base-url sandbox đã bị gỡ — L3-C6-a), nên polling chỉ chạy khi có
 * {@code SEPAY_API_TOKEN}.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SePayPollingScheduler {

    /** Chỉ quét các đơn được tạo trong cửa sổ này. */
    private static final Duration LOOKBACK = Duration.ofHours(24);
    /** Số hàng PENDING tối đa quét mỗi lượt. */
    private static final int BATCH_LIMIT = 20;

    private final PaymentService paymentService;

    /**
     * Chạy một lượt quét mỗi {@code sepay.polling.interval-ms} (mặc định 60s), trễ
     * 15s sau khi khởi động để datasource và token SePay kịp ấm. Mọi lỗi đều được
     * ghi log rồi nuốt đi, để lịch chạy vẫn sống sót qua lỗi tạm thời của SePay
     * hoặc database.
     */
    @Scheduled(fixedDelayString = "${sepay.polling.interval-ms:60000}", initialDelay = 15000)
    public void sweepPendingPayments() {
        try {
            paymentService.sweepPendingPayments(LOOKBACK, BATCH_LIMIT);
        } catch (Exception e) {
            // Không bao giờ để một lỗi tạm thời của SePay/DB giết lịch chạy.
            log.warn("SePay polling sweep failed: {}", e.getMessage());
        }
    }
}
