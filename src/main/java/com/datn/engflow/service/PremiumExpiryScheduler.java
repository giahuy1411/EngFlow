package com.datn.engflow.service;

import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Nightly job that clears the premium flag on accounts whose expiry date has passed.
 *
 * <p>Layer: scheduled infrastructure, invoked by Spring at 02:00 rather than by a
 * controller. It runs one bulk {@code UPDATE} through {@link UserRepository} instead of
 * loading rows, so the check stays cheap as the user table grows; the read path
 * ({@code PaymentService.getPremiumStatus}) also downgrades lazily, so this job is a
 * cleanup for reporting rather than the only enforcement.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PremiumExpiryScheduler {

    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * Downgrades every user whose {@code premiumExpiry} is strictly before today, using
     * the injected {@code Clock} so the cutoff is testable.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkExpiredPremium() {
        int updated = userRepository.updateExpiredPremium(LocalDate.now(clock));
        if (updated > 0) {
            log.info("Downgraded {} expired premium users", updated);
        }
    }
}
