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
 * Mở rộng check constraint của SQL Server cho các trạng thái submission video.
 * Migration an toàn để chạy mỗi lần startup vì nó bỏ qua những database có
 * constraint đã hỗ trợ sẵn các trạng thái chấm tay.
 *
 * <p>{@code ddl-auto=update} của Hibernate KHÔNG nới được một CHECK constraint, nên
 * database tạo trước khi có tính năng chấm tay sẽ từ chối mọi write
 * {@code UNDER_REVIEW} hay {@code GRADED}. Runner dò
 * {@code sys.check_constraints} trước và chỉ áp SQL trong
 * {@code db/migration/V4__expand_video_submission_status.sql} khi định nghĩa hiện
 * tại còn thiếu một trong ba trạng thái.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpeakingSubmissionStatusMigration implements ApplicationRunner {

    /**
     * Đếm xem bảng {@code speaking_submissions} đã tồn tại chưa.
     *
     * <p>Chạy trên database trắng (chưa migrate) thì query này phải chịu được việc
     * bảng chưa có, nên nó hỏi {@code sys.tables} chứ không SELECT thẳng vào bảng.</p>
     */
    static final String TABLE_EXISTS_SQL = """
            SELECT COUNT(*)
            FROM sys.tables
            WHERE object_id = OBJECT_ID(N'dbo.speaking_submissions')
            """;

    /**
     * Đếm số check constraint trên cột {@code status} đã chứa đủ ba trạng thái.
     *
     * <p>Join {@code sys.check_constraints} với {@code sys.columns} để chắc chắn
     * constraint tìm được đúng là constraint của cột {@code status}, rồi so
     * {@code definition} bằng ba mẫu LIKE. Kết quả {@code > 0} nghĩa là đã migrate
     * rồi, không cần chạy SQL nữa.</p>
     */
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

    /** Đường dẫn classpath tới file SQL migration được áp khi constraint còn thiếu. */
    private static final String MIGRATION_RESOURCE =
            "db/migration/V4__expand_video_submission_status.sql";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Áp migration mở rộng status constraint khi thật sự cần.
     *
     * @param args tham số khởi động của ứng dụng
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
     * Chạy một {@code SELECT COUNT(*)} dạng scalar và chuẩn hoá kết quả nullable.
     *
     * @param sql query đếm cần thực thi
     * @return số đếm, hoặc {@code 0} khi query không trả về giá trị nào
     */
    private int queryCount(String sql) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    /**
     * Nạp SQL migration từ classpath.
     *
     * <p>Bọc trong {@link UncheckedIOException} để caller không bị buộc phải khai
     * checked exception mà nó không xử lý được — thiếu resource ở đây nghĩa là build
     * artifact hỏng, và runner nên abort thật to.</p>
     *
     * @return nội dung file dưới dạng chuỗi UTF-8
     * @throws UncheckedIOException nếu resource vắng mặt hoặc không đọc được
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
