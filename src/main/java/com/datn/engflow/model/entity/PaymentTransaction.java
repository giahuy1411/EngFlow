package com.datn.engflow.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dòng giao dịch thanh toán qua SePay: vừa là "đơn chờ" vừa là "bằng chứng đã
 * thanh toán" của cùng một đơn.
 *
 * <p>Tầng entity, chỉ {@link PaymentService} ghi. Một đơn bắt đầu ở
 * {@code status = PENDING} (chưa có {@code transaction_id}); khi webhook hoặc
 * poll SePay khớp, chính dòng đó được cập nhật thành {@code SUCCESS} thay vì
 * chèn dòng thứ hai — nhờ vậy {@code order_code} không bao giờ có hai dòng cùng
 * trạng thái thành công. {@link #webhookRaw} lưu nguyên body đã kiểm chữ ký để
 * đối chiếu khi có khiếu nại.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "order_code", length = 50)
    private String orderCode;

    @Column(precision = 18)
    private BigDecimal amount;

    @Column(length = 50)
    private String gateway;

    @org.hibernate.annotations.Nationalized
    @Column(length = 500)
    private String content;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "plan_type", length = 20)
    private String planType;

    @Column(name = "premium_expiry")
    private LocalDate premiumExpiry;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "webhook_raw", columnDefinition = "NVARCHAR(MAX)")
    private String webhookRaw;
}
