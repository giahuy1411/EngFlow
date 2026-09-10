package com.datn.engflow.service;

import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * class PremiumExpiryScheduler.
 */
public class PremiumExpiryScheduler {

    private final UserRepository userRepository;
    private final Clock clock;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkExpiredPremium() {
        int updated = userRepository.updateExpiredPremium(LocalDate.now(clock));
        if (updated > 0) {
            log.info("Downgraded {} expired premium users", updated);
        }
    }
}
