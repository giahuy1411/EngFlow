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
 * Job chạy hằng đêm để xoá cờ premium của những tài khoản đã quá hạn.
 *
 * <p>Tầng hạ tầng theo lịch, do Spring gọi lúc 02:00 chứ không qua controller. Nó
 * chạy một câu {@code UPDATE} hàng loạt qua {@link UserRepository} thay vì nạp
 * từng hàng, nên chi phí kiểm tra không tăng theo số lượng user; đường đọc
 * ({@code PaymentService.getPremiumStatus}) cũng tự hạ cấp một cách lazy, vì vậy
 * job này là bước dọn dẹp cho báo cáo chứ không phải nơi thực thi duy nhất.
 * Nói cách khác, premium hết hạn vẫn bị chặn đúng ngay cả khi job này chưa kịp
 * chạy.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PremiumExpiryScheduler {

    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * Hạ cấp mọi user có {@code premiumExpiry} trước hôm nay (nghiêm ngặt nhỏ
     * hơn), dùng {@code Clock} được inject để mốc thời gian kiểm thử được.
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
