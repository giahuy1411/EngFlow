package com.datn.engflow.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakingSubmissionStatusMigrationTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void expandsLegacyConstraintWhenManualStatusesAreMissing() {
        when(jdbcTemplate.queryForObject(
                SpeakingSubmissionStatusMigration.TABLE_EXISTS_SQL, Integer.class))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(
                SpeakingSubmissionStatusMigration.MANUAL_STATUS_COUNT_SQL, Integer.class))
                .thenReturn(0);
        SpeakingSubmissionStatusMigration migration =
                new SpeakingSubmissionStatusMigration(jdbcTemplate);

        migration.run(new DefaultApplicationArguments());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).execute(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue())
                .contains("SUBMITTED", "UNDER_REVIEW", "GRADED")
                .contains("UPLOADED", "PROCESSING", "COMPLETED", "FAILED");
    }

    @Test
    void skipsMigrationWhenConstraintAlreadySupportsManualStatuses() {
        when(jdbcTemplate.queryForObject(
                SpeakingSubmissionStatusMigration.TABLE_EXISTS_SQL, Integer.class))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(
                SpeakingSubmissionStatusMigration.MANUAL_STATUS_COUNT_SQL, Integer.class))
                .thenReturn(1);
        SpeakingSubmissionStatusMigration migration =
                new SpeakingSubmissionStatusMigration(jdbcTemplate);

        migration.run(new DefaultApplicationArguments());

        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void skipsMigrationWhenSubmissionTableDoesNotExist() {
        when(jdbcTemplate.queryForObject(
                SpeakingSubmissionStatusMigration.TABLE_EXISTS_SQL, Integer.class))
                .thenReturn(0);
        SpeakingSubmissionStatusMigration migration =
                new SpeakingSubmissionStatusMigration(jdbcTemplate);

        migration.run(new DefaultApplicationArguments());

        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }
}
