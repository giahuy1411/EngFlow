package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nghiệp vụ quyền truy cập và quota AI theo ngày.
 *
 * <p>Quy tắc (theo yêu cầu chủ sản phẩm):
 * <ul>
 *   <li>Admin dùng được mọi chức năng của user thường và cả tính năng Premium.</li>
 *   <li>User thường: 5 lượt sinh từ AI miễn phí cho mỗi NGÀY.</li>
 *   <li>Premium còn hạn và admin: không giới hạn.</li>
 * </ul>
 *
 * <p>Đồng hồ được truyền vào nên ranh giới "một ngày" kiểm soát được trong test.
 */
class UserServicePremiumAndQuotaTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 17);

    private UserService userService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(TODAY.atStartOfDay(VN).toInstant(), VN);
        // Chỉ hasPremiumAccess/quota được gọi trong test này nên các dependency khác
        // không cần thật.
        userService = new UserService(null, null, null, null, null, null, null, clock);
    }

    private User user(Boolean isAdmin, Boolean isPremium, LocalDate expiry) {
        return User.builder()
                .isAdmin(isAdmin)
                .isPremium(isPremium)
                .premiumExpiry(expiry)
                .build();
    }

    // ---------- hasPremiumAccess ----------

    @Test
    @DisplayName("Admin có quyền premium kể cả khi không mua gói nào")
    void adminGetsPremiumAccessWithoutBuying() {
        assertThat(userService.hasPremiumAccess(user(true, false, null))).isTrue();
    }

    @Test
    @DisplayName("Admin hết hạn premium vẫn còn quyền (admin thắng)")
    void adminKeepsAccessEvenWithExpiredPremium() {
        assertThat(userService.hasPremiumAccess(user(true, true, TODAY.minusDays(1)))).isTrue();
    }

    @Test
    @DisplayName("Premium chưa hết hạn có quyền")
    void activePremiumHasAccess() {
        assertThat(userService.hasPremiumAccess(user(false, true, TODAY.plusDays(10)))).isTrue();
    }

    @Test
    @DisplayName("Premium hết hạn mất quyền")
    void expiredPremiumLosesAccess() {
        assertThat(userService.hasPremiumAccess(user(false, true, TODAY.minusDays(1)))).isFalse();
    }

    @Test
    @DisplayName("Hạn premium đúng hôm nay vẫn còn hiệu lực")
    void premiumExpiringTodayStillValid() {
        assertThat(userService.hasPremiumAccess(user(false, true, TODAY))).isTrue();
    }

    @Test
    @DisplayName("Premium không có ngày hết hạn coi như còn hạn")
    void premiumWithoutExpiryIsValid() {
        assertThat(userService.hasPremiumAccess(user(false, true, null))).isTrue();
    }

    @Test
    @DisplayName("User thường không có quyền premium")
    void freeUserHasNoPremiumAccess() {
        assertThat(userService.hasPremiumAccess(user(false, false, null))).isFalse();
    }

    @Test
    @DisplayName("Cờ null không làm nổ, coi như không có quyền")
    void nullFlagsAreTreatedAsNoAccess() {
        assertThat(userService.hasPremiumAccess(user(null, null, null))).isFalse();
    }

    // ---------- quota AI theo ngày ----------

    @Test
    @DisplayName("Cột quota null (user cũ) được tính là 0 lượt cho ngày hôm nay")
    void legacyLifetimeCountIsIgnoredForToday() {
        User legacy = user(false, false, null);
        legacy.setAiGenerationCount(5); // từng dùng hết 5 lượt trước khi đổi sang quota ngày
        legacy.setAiQuotaDate(null);

        assertThat(userService.aiGenerationsUsedToday(legacy)).isZero();
        assertThat(userService.hasAiGenerationQuota(legacy)).isTrue();
    }

    @Test
    @DisplayName("Đếm lượt đã dùng đúng khi ngày quota khớp hôm nay")
    void countsUsedWhenQuotaDateIsToday() {
        User u = user(false, false, null);
        u.setAiGenerationCount(3);
        u.setAiQuotaDate(TODAY);

        assertThat(userService.aiGenerationsUsedToday(u)).isEqualTo(3);
        assertThat(userService.hasAiGenerationQuota(u)).isTrue();
    }

    @Test
    @DisplayName("Dùng đủ 5 lượt trong ngày thì hết quota")
    void quotaExhaustedAtLimit() {
        User u = user(false, false, null);
        u.setAiGenerationCount(UserService.AI_GENERATIONS_PER_DAY);
        u.setAiQuotaDate(TODAY);

        assertThat(userService.hasAiGenerationQuota(u)).isFalse();
        assertThat(userService.remainingAiGenerations(u)).isZero();
    }

    @Test
    @DisplayName("Qua ngày khác thì quota tự reset về 5 lượt")
    void quotaResetsNextDay() {
        User yesterday = user(false, false, null);
        yesterday.setAiGenerationCount(5);
        yesterday.setAiQuotaDate(TODAY.minusDays(1));

        assertThat(userService.aiGenerationsUsedToday(yesterday)).isZero();
        assertThat(userService.remainingAiGenerations(yesterday))
                .isEqualTo(UserService.AI_GENERATIONS_PER_DAY);
    }

    @Test
    @DisplayName("Premium và admin còn quyền không giới hạn, không tính quota")
    void premiumAndAdminAreUnlimited() {
        User premium = user(false, true, TODAY.plusDays(1));
        premium.setAiGenerationCount(5);
        premium.setAiQuotaDate(TODAY);

        User admin = user(true, false, null);
        admin.setAiGenerationCount(99);
        admin.setAiQuotaDate(TODAY);

        assertThat(userService.hasAiGenerationQuota(premium)).isTrue();
        assertThat(userService.hasAiGenerationQuota(admin)).isTrue();
        assertThat(userService.remainingAiGenerations(premium)).isNull();
        assertThat(userService.remainingAiGenerations(admin)).isNull();
    }

    @Test
    @DisplayName("Null count không làm nổ")
    void nullCountTreatedAsZero() {
        User u = user(false, false, null);
        u.setAiGenerationCount(null);
        u.setAiQuotaDate(TODAY);

        assertThat(userService.aiGenerationsUsedToday(u)).isZero();
        assertThat(userService.remainingAiGenerations(u)).isEqualTo(UserService.AI_GENERATIONS_PER_DAY);
    }

    @Test
    @DisplayName("Value quota âm bất thường không bị coi là còn hạn mức dương")
    void negativeCountClampedToZero() {
        User u = user(false, false, null);
        u.setAiGenerationCount(-7);
        u.setAiQuotaDate(TODAY);

        assertThat(userService.aiGenerationsUsedToday(u)).isZero();
    }

    // ---------- consumeAiGenerationQuota (đường ghi) ----------

    @Test
    @DisplayName("User cũ từng dùng hết 5 lượt vĩnh viễn: lượt đầu hôm nay ghi thành 1")
    void firstUseOfTheDayStartsFromOneForLegacyUser() {
        UserRepository repository = org.mockito.Mockito.mock(UserRepository.class);
        UserService svc = new UserService(
                repository, null, null, null, null, null, null, clock);
        User legacy = user(false, false, null);
        legacy.setAiGenerationCount(5); // di sản của bộ đếm vĩnh viễn
        legacy.setAiQuotaDate(null);

        svc.consumeAiGenerationQuota(legacy);

        assertThat(legacy.getAiQuotaDate()).isEqualTo(TODAY);
        assertThat(legacy.getAiGenerationCount()).isEqualTo(1);
        org.mockito.Mockito.verify(repository).save(legacy);
    }

    @Test
    @DisplayName("Cùng ngày thì cộng dồn, không reset")
    void accumulatesWithinSameDay() {
        UserRepository repository = org.mockito.Mockito.mock(UserRepository.class);
        UserService svc = new UserService(
                repository, null, null, null, null, null, null, clock);
        User u = user(false, false, null);
        u.setAiGenerationCount(2);
        u.setAiQuotaDate(TODAY);

        svc.consumeAiGenerationQuota(u);

        assertThat(u.getAiGenerationCount()).isEqualTo(3);
        assertThat(u.getAiQuotaDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("Sang ngày mới thì bộ đếm reset về 1 và cập nhật ngày quota")
    void rollsOverOnNewDay() {
        UserRepository repository = org.mockito.Mockito.mock(UserRepository.class);
        UserService svc = new UserService(
                repository, null, null, null, null, null, null, clock);
        User u = user(false, false, null);
        u.setAiGenerationCount(5);
        u.setAiQuotaDate(TODAY.minusDays(1));

        svc.consumeAiGenerationQuota(u);

        assertThat(u.getAiQuotaDate()).isEqualTo(TODAY);
        assertThat(u.getAiGenerationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Premium và admin không bị ghi nhận lượt, không lưu để đếm")
    void doesNotCountForPremiumOrAdmin() {
        UserRepository repository = org.mockito.Mockito.mock(UserRepository.class);
        UserService svc = new UserService(
                repository, null, null, null, null, null, null, clock);
        User premium = user(false, true, TODAY.plusDays(5));
        premium.setAiGenerationCount(0);
        premium.setAiQuotaDate(TODAY);
        User admin = user(true, false, null);
        admin.setAiGenerationCount(0);

        svc.consumeAiGenerationQuota(premium);
        svc.consumeAiGenerationQuota(admin);

        assertThat(premium.getAiGenerationCount()).isZero();
        assertThat(admin.getAiGenerationCount()).isZero();
        org.mockito.Mockito.verifyNoInteractions(repository);
    }
}
