package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Trạng thái quyền premium của tài khoản.
 *
 * <p>Hiện chưa có controller nào trả kiểu này — đường thanh toán SePay chỉ có
 * {@code POST /api/webhook/sepay} trả map thô. DTO được giữ lại cho tầng response
 * của luồng premium.
 */
@Data
@Builder
public class PaymentStatusResponse {
    private boolean isPremium;
    private LocalDate premiumExpiry;
}
