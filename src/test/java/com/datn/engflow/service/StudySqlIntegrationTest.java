package com.datn.engflow.service;

import com.datn.engflow.model.entity.StudyDay;
import com.datn.engflow.model.entity.StudyPolicy;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.StudyDayRepository;
import com.datn.engflow.repository.StudyPolicyRepository;
import com.datn.engflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = StudySqlIntegrationTest.IsolatedJpa.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.location=optional:classpath:/study-isolated.properties",
        "spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=engflow_study_test;encrypt=true;trustServerCertificate=true",
        "spring.datasource.username=engflow_study_test",
        "spring.datasource.password=${ENGFLOW_STUDY_TEST_PASSWORD}",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "spring.jpa.open-in-view=false",
        "spring.data.redis.repositories.enabled=false"})
@EnabledIfEnvironmentVariable(named = "ENGFLOW_STUDY_TEST_PASSWORD", matches = ".+")
class StudySqlIntegrationTest {
    @Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.boot.persistence.autoconfigure.EntityScan("com.datn.engflow.model.entity")
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories("com.datn.engflow.repository")
    @Import({StudyActivityService.class, SrsService.class})
    static class IsolatedJpa {
        @Bean Clock clock() { return mock(Clock.class); }
        @Bean StringRedisTemplate redis() { return mock(StringRedisTemplate.class); }
    }

    @Autowired private StudyActivityService service;
    @Autowired private StudyDayRepository days;
    @Autowired private StudyPolicyRepository policies;
    @Autowired private UserRepository users;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private Clock clock;
    @Autowired private javax.sql.DataSource dataSource;
    @Autowired private SrsService srs;
    @Autowired private com.datn.engflow.repository.VocabularyRepository vocabulary;
    @Autowired private com.datn.engflow.repository.UserVocabularyProgressRepository progress;
    private final LocalDate start = LocalDate.of(2026, 9, 18);
    private final ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
    private TransactionTemplate transaction;
    private Long userId;

    private void at(LocalDate date) {
        when(clock.withZone(zone)).thenReturn(Clock.fixed(date.atStartOfDay(zone).toInstant(), zone));
    }

    @BeforeEach
    void setUp() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getCatalog()).isEqualTo("engflow_study_test");
        }
        transaction = new TransactionTemplate(transactionManager);
        policies.saveAndFlush(new StudyPolicy(1, start));
        String suffix = UUID.randomUUID().toString().replace("-", "");
        userId = users.saveAndFlush(User.builder().username("study_" + suffix)
                .email(suffix + "@example.test").passwordHash("test-only")
                .currentStreak(99).lastStudyDate(start.minusDays(1)).build()).getId();
        at(start);
    }

    @Test
    void fourDayJourneyPersistsAndReadsActualSql() {
        transaction.executeWithoutResult(status -> service.recordStudy(userId));
        assertThat(service.snapshot(userId, 30).currentStreak()).isEqualTo(1);
        at(start.plusDays(1));
        assertThat(service.snapshot(userId, 30).currentStreak()).isEqualTo(1);
        at(start.plusDays(2));
        assertThat(service.snapshot(userId, 30).currentStreak()).isZero();
        transaction.executeWithoutResult(status -> service.recordStudy(userId));
        at(start.plusDays(3));
        transaction.executeWithoutResult(status -> service.recordStudy(userId));
        assertThat(service.snapshot(userId, 30).currentStreak()).isEqualTo(2);
        assertThat(service.snapshot(userId, 30).studiedDays())
                .containsExactly(start, start.plusDays(2), start.plusDays(3));
    }

    @Test
    void rollbackRemovesBothResultChangeAndStudyDay() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            var user = users.findById(userId).orElseThrow();
            user.setTotalPoints(5);
            users.saveAndFlush(user);
            service.recordStudy(userId);
            days.flush();
            throw new IllegalStateException("forced rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(days.existsByUserIdAndStudyDate(userId, start)).isFalse();
        assertThat(users.findById(userId).orElseThrow().getTotalPoints()).isZero();
    }

    @Test
    void concurrentActivitiesCommitExactlyOneDay() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Void> activity = () -> {
                ready.countDown();
                if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("barrier timeout");
                transaction.executeWithoutResult(status -> service.recordStudy(userId));
                return null;
            };
            var first = executor.submit(activity);
            var second = executor.submit(activity);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        assertThat(days.findDates(userId, start, start)).containsExactly(start);
    }

    @Test
    void uniqueConstraintRejectsDuplicateAndServiceRequiresTransaction() {
        days.saveAndFlush(new StudyDay(userId, start));
        assertThatThrownBy(() -> days.saveAndFlush(new StudyDay(userId, start)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> service.recordStudy(userId))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    @Test
    void srsEndpointCommitsProgressAndStudyDay() {
        var word = vocabulary.saveAndFlush(com.datn.engflow.model.entity.Vocabulary.builder()
                .word("study" + userId).meaning("học").build());
        var principal = new com.datn.engflow.security.UserPrincipal(users.findById(userId).orElseThrow());
        var endpoint = new com.datn.engflow.controller.SrsController(srs);

        var response = endpoint.reviewWord(principal, java.util.Map.of(
                "vocabId", Math.toIntExact(word.getId()), "quality", 0));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(progress.findByUserIdAndVocabularyId(userId, word.getId()).orElseThrow().getReviewCount())
                .isEqualTo(1);
        assertThat(days.existsByUserIdAndStudyDate(userId, start)).isTrue();
        assertThat(service.snapshot(userId, 30).currentStreak()).isEqualTo(1);
    }

    @Test
    void studyWriteFailureRollsBackActualSrsProgress() {
        var word = vocabulary.saveAndFlush(com.datn.engflow.model.entity.Vocabulary.builder()
                .word("rollback" + userId).meaning("học").build());
        // audit-v10 F122: nhánh "trước cutover" không còn ném lỗi (nó là trạng thái cấu
        // hình hợp lệ), nên phải tiêm lỗi qua một thất bại thật khác — user inactive.
        // Ý định của test này là tính nguyên tử khi ghi study thất bại, không phải ngày
        // cutover, nên đổi nguồn lỗi mà giữ nguyên ngữ nghĩa kiểm.
        var user = users.findById(userId).orElseThrow();
        user.setIsActive(false);
        users.saveAndFlush(user);

        assertThatThrownBy(() -> srs.reviewWord(userId, word.getId(), 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Inactive users cannot record study activity");

        assertThat(progress.findByUserIdAndVocabularyId(userId, word.getId())).isEmpty();
        assertThat(days.findDates(userId, start.minusDays(1), start)).isEmpty();
    }

    @Test
    void beforeCutoverRecordsNoStudyDayAndDoesNotFailTheActivity() {
        var word = vocabulary.saveAndFlush(com.datn.engflow.model.entity.Vocabulary.builder()
                .word("precutover" + userId).meaning("học").build());
        at(start.minusDays(1));

        // Không ném: streak chỉ đang ngủ, hoạt động học vẫn phải thành công.
        srs.reviewWord(userId, word.getId(), 5);

        assertThat(progress.findByUserIdAndVocabularyId(userId, word.getId())).isPresent();
        assertThat(days.findDates(userId, start.minusDays(1), start)).isEmpty();
    }
}
