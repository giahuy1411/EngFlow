package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
/**
 * class PaymentStatusResponse.
 */
public class PaymentStatusResponse {
    private boolean isPremium;
    private LocalDate premiumExpiry;
}
