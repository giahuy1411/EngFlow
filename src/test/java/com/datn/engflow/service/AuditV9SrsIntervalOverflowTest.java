package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.UserVocabularyProgress;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.UserVocabularyProgressRepository;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * audit-v9 F106 (regression for the live 500 on POST /api/srs/review).
 *
 * <p>{@code SrsService.reviewWord} runs SM-2 without an upper bound: every review with
 * quality &gt;= 3 multiplies the stored interval by the ease factor. Measured on
 * 2026-09-17: row id=20002 (user 2 / vocabulary 10017) reached
 * {@code repetitions=15, srs_interval=1_537_216}. The next review computed
 * {@code next_review_date = now().plusDays(3_996_762)} (~year 13000), which is outside
 * the {@code datetime2} range (max 9999-12-31). The UPDATE therefore threw
 * {@code SQLServerException: One or more values is out of range of values for the
 * datetime2 SQL Server data type.}, surfaced to the learner as HTTP 500 - and it stays
 * 500 forever, because the stored interval is never repaired. Reproduced live:
 * {@code POST /api/srs/review {"vocabId":10017,"quality":5}} -&gt; 500.
 *
 * <p>Fix under test: cap the interval at {@code SrsService.MAX_INTERVAL_DAYS} (365, the
 * standard SM-2 ceiling). These tests fail on the pre-fix code because the explicit
 * {@code flush()} forces the UPDATE that the production commit would run.
 */
@SpringBootTest(classes = AuditV9SrsIntervalOverflowTest.IsolatedJpaConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.config.location=optional:classpath:/audit-isolated.properties",
                "spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=english_learning_audit_v9_srs;encrypt=true;trustServerCertificate=true",
                "spring.datasource.username=audit_v9_srs",
                "spring.datasource.password=${ENGFLOW_AUDIT_JPA_PASSWORD}",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never",
                "spring.jpa.open-in-view=false",
                "spring.main.allow-bean-definition-overriding=false"
        })
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "ENGFLOW_AUDIT_JPA_PASSWORD", matches = ".+")
@Transactional
class AuditV9SrsIntervalOverflowTest {

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.boot.persistence.autoconfigure.EntityScan("com.datn.engflow.model.entity")
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories("com.datn.engflow.repository")
    @org.springframework.context.annotation.Import(SrsService.class)
    static class IsolatedJpaConfiguration {
        @org.springframework.context.annotation.Bean
        StudyActivityService studyActivityService() {
            return org.mockito.Mockito.mock(StudyActivityService.class);
        }
    }

    private static final int HARD_MAX_DAYS = 365;

    @Autowired
    private SrsService srsService;

    @Autowired
    private UserVocabularyProgressRepository progressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    private Long userId;
    private Long vocabId;

    @BeforeEach
    void seedFixtures() {
        User user = userRepository.save(User.builder()
                .username("zzsrsoverflow")
                .email("zzsrsoverflow@test.local")
                .passwordHash("not-a-real-hash")
                .isAdmin(false)
                .build());
        Vocabulary vocab = vocabularyRepository.save(Vocabulary.builder()
                .word("zzsrsoverflowword")
                .meaning("probe")
                .build());
        userId = user.getId();
        vocabId = vocab.getId();
    }

    private void seedProgress(int repetitions, int interval, double easeFactor) {
        progressRepository.save(UserVocabularyProgress.builder()
                .user(userRepository.getReferenceById(userId))
                .vocabulary(vocabularyRepository.getReferenceById(vocabId))
                .repetitions(repetitions)
                .interval(interval)
                .easeFactor(easeFactor)
                .reviewCount(42)
                .masteryLevel(2)
                .build());
        progressRepository.flush();
    }

    /** The exact live-broken state: next review must not throw a datetime2 overflow. */
    @Test
    void reviewOnStoredHugeInterval_doesNotOverflowDatetime2() {
        seedProgress(15, 1_537_216, 2.6);

        assertDoesNotThrow(() -> {
            srsService.reviewWord(userId, vocabId, 5);
            progressRepository.flush();
        });

        UserVocabularyProgress after = progressRepository
                .findByUserIdAndVocabularyId(userId, vocabId).orElseThrow();
        assertTrue(after.getInterval() <= HARD_MAX_DAYS,
                "interval must be capped, was " + after.getInterval());
        assertNotNull(after.getNextReviewDate());
        assertTrue(after.getNextReviewDate().isBefore(LocalDate.of(2100, 1, 1).atStartOfDay()),
                "next_review_date must stay in datetime2 range, was " + after.getNextReviewDate());
    }

    /** A learner who keeps answering perfectly must never grow out of range. */
    @Test
    void repeatedPerfectReviews_neverExceedIntervalCap() {
        for (int i = 0; i < 40; i++) {
            assertDoesNotThrow(() -> {
                srsService.reviewWord(userId, vocabId, 5);
                progressRepository.flush();
            });
        }

        UserVocabularyProgress after = progressRepository
                .findByUserIdAndVocabularyId(userId, vocabId).orElseThrow();
        assertTrue(after.getInterval() <= HARD_MAX_DAYS,
                "interval must be capped after 40 perfect reviews, was " + after.getInterval());
        assertTrue(after.getNextReviewDate().isBefore(LocalDate.of(2100, 1, 1).atStartOfDay()),
                "next_review_date must stay in range, was " + after.getNextReviewDate());
    }

    /** A failing answer still resets to the short interval (no behaviour change). */
    @Test
    void failedReview_resetsToShortInterval() {
        seedProgress(15, 1_537_216, 2.6);

        assertDoesNotThrow(() -> {
            srsService.reviewWord(userId, vocabId, 1);
            progressRepository.flush();
        });

        UserVocabularyProgress after = progressRepository
                .findByUserIdAndVocabularyId(userId, vocabId).orElseThrow();
        assertTrue(after.getInterval() == 1, "quality<3 must reset interval to 1, was " + after.getInterval());
        assertTrue(after.getRepetitions() == 0, "quality<3 must reset repetitions to 0");
    }
}
