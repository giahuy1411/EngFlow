package com.datn.engflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Periodic SePay polling fallback. The webhook is the primary payment-
 * confirmation channel, but during local development it depends on a public
 * tunnel (Tailscale Funnel / ngrok) that may be down, misconfigured, or
 * unreachable from SePay's side. This scheduler makes the API polling a true
 * fallback: it sweeps recent PENDING orders on its own, so a transfer is
 * activated within ~1 minute even if the user never revisits the premium page.
 *
 * <p>Cost guard: each sweep performs at most one SePay User API call per
 * pending order (capped), well within the documented rate limit. Orders older
 * than the lookback window are ignored — they are effectively abandoned.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SePayPollingScheduler {

    /** Only sweep orders created within this window. */
    private static final Duration LOOKBACK = Duration.ofHours(24);
    /** Max PENDING rows scanned per sweep. */
    private static final int BATCH_LIMIT = 20;

    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${sepay.polling.interval-ms:60000}", initialDelay = 15000)
    public void sweepPendingPayments() {
        try {
            paymentService.sweepPendingPayments(LOOKBACK, BATCH_LIMIT);
        } catch (Exception e) {
            // Never let a transient SePay/DB error kill the schedule.
            log.warn("SePay polling sweep failed: {}", e.getMessage());
        }
    }
}
