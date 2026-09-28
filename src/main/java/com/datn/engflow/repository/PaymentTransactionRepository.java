package com.datn.engflow.repository;

import com.datn.engflow.model.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Truy cập bảng {@code payment_transactions} — sổ giao dịch của luồng SePay.
 *
 * <p>Một đơn có thể đi qua hai đường: webhook SePay đẩy tới, và scheduler
 * {@code SePayPollingScheduler} chủ động hỏi lại các đơn còn PENDING. Hai đường có thể
 * chồng lấn nên mọi thao tác chốt đơn phải idempotent — vì vậy ở đây có sẵn các truy vấn
 * kiểm tra "đã SUCCESS chưa" và "bản PENDING mới nhất là bản nào".
 *
 * <p>Lưu ý cho sweep/parity: các dòng SUCCESS là dữ liệu thật, baseline đối chiếu dựa
 * trên chúng — dọn dẹp phải liệt kê id và bật {@code SET QUOTED_IDENTIFIER ON}
 * (DB có filtered index, thiếu là {@code Msg 1934} dù sqlcmd vẫn exit 0).
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    /**
     * Tra theo mã giao dịch của cổng thanh toán — khoá chống trùng khi webhook gọi lại
     * cùng một giao dịch.
     */
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
    /**
     * Tra theo mã đơn của hệ thống. Hiện chưa có call site (0 caller) — luồng thật dùng
     * {@link #findFirstByOrderCodeAndStatusOrderByIdDesc} để chỉ lấy bản PENDING mới nhất.
     */
    Optional<PaymentTransaction> findByOrderCode(String orderCode);
    /**
     * Bản ghi mới nhất theo id của một (orderCode, status). Lấy bản mới nhất thay vì
     * "bản bất kỳ" để đơn bị tạo lại nhiều lần vẫn chốt đúng bản đang mở.
     */
    Optional<PaymentTransaction> findFirstByOrderCodeAndStatusOrderByIdDesc(String orderCode, String status);
    /**
     * Đã có bản ghi ở trạng thái này cho đơn chưa — dùng làm chốt idempotent
     * (điển hình là kiểm tra {@code SUCCESS} trước khi cộng premium).
     */
    boolean existsByOrderCodeAndStatus(String orderCode, String status);
    /**
     * Tối đa 10 đơn gần nhất của user theo trạng thái — phục vụ màn lịch sử thanh toán
     * của người dùng, cố tình chặn trần để không quét toàn bộ lịch sử.
     */
    List<PaymentTransaction> findTop10ByUserIdAndStatusOrderByIdDesc(Long userId, String status);

    /**
     * Các đơn PENDING tạo từ {@code since} trở đi, mới nhất trước, chặn trần {@code max} dòng.
     * Dùng cho fallback polling nền của SePay nên chỉ quét đơn gần đây thay vì mọi dòng
     * PENDING từng tạo.
     *
     * <p>Cảnh báo: tham số {@code max} KHÔNG được dịch thành {@code LIMIT} — JPQL không
     * hỗ trợ trần động trong câu lệnh, nên giới hạn thật do tầng gọi áp
     * ({@code SePayPollingScheduler} truyền {@code BATCH_LIMIT} và dừng vòng lặp theo đó).
     */
    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = 'PENDING' AND t.createdAt >= :since ORDER BY t.id DESC")
    List<PaymentTransaction> findRecentPending(@Param("since") LocalDateTime since, @Param("max") int max);
}
