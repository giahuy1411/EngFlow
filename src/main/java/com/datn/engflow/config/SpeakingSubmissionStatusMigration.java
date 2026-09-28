package com.datn.engflow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Expands the SQL Server check constraint for video submission statuses.
 * The migration is safe to run on every startup because it skips databases
 * whose constraint already supports the manual-grading states.
 *
 * <p>Hibernate's {@code ddl-auto=update} cannot widen a CHECK constraint, so a
 * database created before manual grading existed would reject every
 * {@code UNDER_REVIEW} or {@code GRADED} write. The runner probes
 * {@code sys.check_constraints} first and only applies the SQL in
 * {@code db/migration/V4__expand_video_submission_status.sql} when the current
 * definition is missing one of the three states.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpeakingSubmissionStatusMigration implements ApplicationRunner {

    static final String TABLE_EXISTS_SQL = """
            SELECT COUNT(*)
            FROM sys.tables
            WHERE object_id = OBJECT_ID(N'dbo.speaking_submissions')
            """;

    static final String MANUAL_STATUS_COUNT_SQL = """
            SELECT COUNT(*)
            FROM sys.check_constraints AS checkConstraint
            JOIN sys.columns AS checkedColumn
                ON checkedColumn.object_id = checkConstraint.parent_object_id
                AND checkedColumn.column_id = checkConstraint.parent_column_id
            WHERE checkConstraint.parent_object_id = OBJECT_ID(N'dbo.speaking_submissions')
              AND checkedColumn.name = N'status'
              AND checkConstraint.definition LIKE '%SUBMITTED%'
              AND checkConstraint.definition LIKE '%UNDER_REVIEW%'
              AND checkConstraint.definition LIKE '%GRADED%'
            """;

    private static final String MIGRATION_RESOURCE =
            "db/migration/V4__expand_video_submission_status.sql";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Applies the expand-only status constraint migration when required.
     *
     * @param args application startup arguments
     */
    @Override
    public void run(ApplicationArguments args) {
        if (queryCount(TABLE_EXISTS_SQL) == 0) {
            log.debug("Skipping video submission status migration: table does not exist");
            return;
        }
        if (queryCount(MANUAL_STATUS_COUNT_SQL) > 0) {
            log.debug("Video submission status constraint already supports manual grading");
            return;
        }

        jdbcTemplate.execute(readMigrationSql());
        log.info("Expanded video submission status constraint for manual grading");
    }

    /**
     * Runs a scalar {@code SELECT COUNT(*)} and normalises the nullable result.
     *
     * @param sql counting query to execute
     * @return the count, or {@code 0} when the query returned no value
     */
    private int queryCount(String sql) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    /**
     * Loads the migration SQL from the classpath.
     *
     * <p>Wrapped in {@link UncheckedIOException} so the caller is not forced to
     * declare a checked exception it cannot act on — a missing resource here
     * means a broken build artifact, and the runner should abort loudly.</p>
     *
     * @return the file contents as a UTF-8 string
     * @throws UncheckedIOException if the resource is absent or unreadable
     */
    private String readMigrationSql() {
        try {
            return new ClassPathResource(MIGRATION_RESOURCE)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Cannot read video submission status migration", exception);
        }
    }
}
