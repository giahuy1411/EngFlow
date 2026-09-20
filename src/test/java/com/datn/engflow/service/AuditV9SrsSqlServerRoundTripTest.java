package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.UserVocabularyProgress;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.UserVocabularyProgressRepository;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(named = "ENGFLOW_AUDIT_SQL_ROUNDTRIP", matches = "true")
class AuditV9SrsSqlServerRoundTripTest {
    @Test
    void cappedServiceOutputRoundTripsDatetime2WhileOldFormulaIsRejected() throws Exception {
        var progressRepository = mock(UserVocabularyProgressRepository.class);
        var userRepository = mock(UserRepository.class);
        var vocabularyRepository = mock(VocabularyRepository.class);
        var service = new SrsService(progressRepository, userRepository, vocabularyRepository,
                mock(DeckWordRepository.class), mock(StudyActivityService.class));
        var user = User.builder().id(1L).build();
        var vocabulary = Vocabulary.builder().id(1L).build();
        var progress = UserVocabularyProgress.builder().user(user).vocabulary(vocabulary)
                .interval(1_537_216).easeFactor(2.6).repetitions(15).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(vocabularyRepository.findById(1L)).thenReturn(Optional.of(vocabulary));
        when(progressRepository.findByUserIdAndVocabularyId(1L, 1L)).thenReturn(Optional.of(progress));

        service.reviewWord(1L, 1L, 5);

        verify(progressRepository).save(progress);
        assertEquals(365, progress.getInterval());
        String url = "jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=true;trustServerCertificate=true";
        try (var connection = DriverManager.getConnection(url, "sa",
                System.getenv("ENGFLOW_AUDIT_SQL_PASSWORD"))) {
            assertEquals("master", connection.getCatalog());
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE #audit_v9_srs_roundtrip (interval_days int, next_review datetime2(7))");
            }
            try {
                try (var insert = connection.prepareStatement(
                        "INSERT INTO #audit_v9_srs_roundtrip VALUES (?, ?)")) {
                    insert.setInt(1, progress.getInterval());
                    insert.setTimestamp(2, Timestamp.valueOf(progress.getNextReviewDate()));
                    assertEquals(1, insert.executeUpdate());
                }
                try (var statement = connection.createStatement();
                     var rows = statement.executeQuery("SELECT interval_days, next_review FROM #audit_v9_srs_roundtrip")) {
                    org.junit.jupiter.api.Assertions.assertTrue(rows.next());
                    assertEquals(365, rows.getInt(1));
                    long roundTripDelta = java.time.Duration.between(progress.getNextReviewDate(),
                            rows.getTimestamp(2).toLocalDateTime()).toNanos();
                    org.junit.jupiter.api.Assertions.assertTrue(Math.abs(roundTripDelta) <= 100);
                    assertFalse(rows.next());
                }
                long uncappedInterval = Math.round(1_537_216 * 2.6);
                LocalDateTime uncappedDate = LocalDateTime.now().plusDays(uncappedInterval);
                org.junit.jupiter.api.Assertions.assertTrue(uncappedDate.getYear() > 9999);
                SQLException rejection = assertThrows(SQLException.class, () -> {
                    try (var insert = connection.prepareStatement(
                            "INSERT INTO #audit_v9_srs_roundtrip VALUES (?, ?)")) {
                        insert.setLong(1, uncappedInterval);
                        insert.setTimestamp(2, Timestamp.valueOf(uncappedDate));
                        insert.executeUpdate();
                    }
                });
                String reason = rejection.getMessage().toLowerCase(java.util.Locale.ROOT);
                org.junit.jupiter.api.Assertions.assertTrue(reason.contains("range") || reason.contains("datetime"));
            } finally {
                try (var statement = connection.createStatement()) {
                    statement.execute("DROP TABLE #audit_v9_srs_roundtrip");
                }
            }
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT OBJECT_ID('tempdb..#audit_v9_srs_roundtrip')")) {
                org.junit.jupiter.api.Assertions.assertTrue(rows.next());
                org.junit.jupiter.api.Assertions.assertNull(rows.getObject(1));
            }
        }
    }
}
