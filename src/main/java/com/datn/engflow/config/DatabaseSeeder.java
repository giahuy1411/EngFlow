package com.datn.engflow.config;

import com.datn.engflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.core.annotation.Order;

/**
 * Startup runner that prepares the schema and puts the database into a usable
 * state on first boot.
 *
 * <p>Three steps run in a fixed order on every startup: drop a legacy CHECK
 * constraint that Hibernate cannot express, rewrite retired enum values in place,
 * then seed {@code data.sql} if the user table is empty. Two demo accounts are
 * ensured last, regardless of whether {@code data.sql} ran. Ordering
 * {@code @Order(1)} puts it ahead of the other seeders, which assume the tables
 * and users already exist.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    /**
     * Runs the schema fixes, the optional {@code data.sql} seed, and the demo
     * account bootstrap.
     *
     * <p>Only the empty-database branch reads {@code data.sql}; on a populated
     * database the file is skipped entirely. Failures from the seed are logged
     * and swallowed so a malformed file cannot abort application startup.</p>
     *
     * @param args command-line arguments supplied to the application
     * @throws Exception if the classpath resource for {@code data.sql} cannot be read
     */
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        dropCurrentLevelCheckConstraint();
        migrateLegacyEnumValues();
        if (userRepository.count() == 0) {
            log.info("Database is empty. Executing data.sql to seed initial data...");
            try {
                ClassPathResource resource = new ClassPathResource("data.sql");
                try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    String sql = FileCopyUtils.copyToString(reader);
                    // Remove BOM if present
                    if (sql.startsWith("\uFEFF")) {
                        sql = sql.substring(1);
                    }
                    jdbcTemplate.execute(sql);
                    log.info("Seed data executed successfully!");
                }
            } catch (Exception e) {
                log.error("Failed to execute data.sql: {}", e.getMessage(), e);
            }
        } else {
            log.info("Database already contains data. Skipping execution of data.sql.");
        }
        
        // Ensure default users exist. Passwords come from environment variables (never hardcoded).
        ensureDefaultUsers();
    }

    /**
     * Drops the SQL Server CHECK constraint on {@code users.current_level}, if any.
     *
     * <p>Older schemas constrained the column to a literal list of level names;
     * once a new level is added, Hibernate's {@code ddl-auto=update} cannot
     * widen it and every write fails. The constraint name is looked up from
     * {@code sys.check_constraints} rather than hardcoded because SQL Server
     * auto-generates it. Missing constraint is the normal case, not an error.</p>
     */
    private void dropCurrentLevelCheckConstraint() {
        try {
            String sql = "DECLARE @constraintName NVARCHAR(128) = (SELECT cc.name FROM sys.check_constraints cc " +
                "JOIN sys.columns c ON cc.parent_column_id = c.column_id AND cc.parent_object_id = c.object_id " +
                "WHERE OBJECT_NAME(cc.parent_object_id) = 'users' AND c.name = 'current_level'); " +
                "IF @constraintName IS NOT NULL EXEC('ALTER TABLE users DROP CONSTRAINT ' + @constraintName)";
            jdbcTemplate.update(sql);
            log.info("Dropped CHECK constraint on users.current_level");
        } catch (Exception e) {
            log.info("No CHECK constraint found on users.current_level: {}", e.getMessage());
        }
    }

    /**
     * Rewrites retired level names in {@code users} and {@code lessons} rows.
     *
     * <p>Two renames are applied: {@code BEGINNER} to {@code ELEMENTARY} and
     * {@code ADVANCED} to {@code UPPER_INTERMEDIATE}. Without this, rows written by
     * an older build map to no {@code LessonLevel} constant and fail on read. Each
     * UPDATE is independent and a failure is logged, so one broken table does not
     * stop the others.</p>
     */
    private void migrateLegacyEnumValues() {
        try {
            int updated = jdbcTemplate.update("UPDATE users SET current_level = 'ELEMENTARY' WHERE current_level = 'BEGINNER'");
            if (updated > 0) log.info("Migrated {} user(s) from BEGINNER to ELEMENTARY", updated);
            updated = jdbcTemplate.update("UPDATE users SET current_level = 'UPPER_INTERMEDIATE' WHERE current_level = 'ADVANCED'");
            if (updated > 0) log.info("Migrated {} user(s) from ADVANCED to UPPER_INTERMEDIATE", updated);
            updated = jdbcTemplate.update("UPDATE lessons SET level = 'ELEMENTARY' WHERE level = 'BEGINNER'");
            if (updated > 0) log.info("Migrated {} lesson(s) from BEGINNER to ELEMENTARY", updated);
            updated = jdbcTemplate.update("UPDATE lessons SET level = 'UPPER_INTERMEDIATE' WHERE level = 'ADVANCED'");
            if (updated > 0) log.info("Migrated {} lesson(s) from ADVANCED to UPPER_INTERMEDIATE", updated);
        } catch (Exception e) {
            log.warn("Failed to migrate legacy enum values: {}", e.getMessage());
        }
    }

    /**
     * Creates the demo student and admin accounts if they are missing.
     *
     * <p>Passwords come from {@code DEFAULT_USER_PASSWORD} and
     * {@code DEFAULT_ADMIN_PASSWORD}, falling back to a well-known value. The
     * warning below fires whenever that fallback is in effect, so a deployer
     * sees it in the log before the accounts become reachable.</p>
     */
    private void ensureDefaultUsers() {
        String userPassword = System.getenv().getOrDefault("DEFAULT_USER_PASSWORD", "password123");
        String adminPassword = System.getenv().getOrDefault("DEFAULT_ADMIN_PASSWORD", "password123");
        // audit-v7 F58: dev/demo seed (AGENTS.md) dùng cặp password công khai. Khi
        // deploy thật phải set 2 env trên; nếu không, chỉ ra log rằng tài khoản
        // đang mang mật khẩu mặc định ai cũng biết.
        if ("password123".equals(adminPassword) || "password123".equals(userPassword)) {
            log.warn("SECURITY: default seed accounts use the fallback password 'password123'."
                    + " Set DEFAULT_ADMIN_PASSWORD / DEFAULT_USER_PASSWORD before any non-local deploy.");
        }
        ensureUserExists("user@gmail.com", "student", userPassword, "Học Viên Mẫu", 
                false, com.datn.engflow.model.enums.LessonLevel.ELEMENTARY, 
                "https://api.dicebear.com/7.x/adventurer/svg?seed=student");
        ensureUserExists("admin@gmail.com", "administrator", adminPassword, "Quản Trị Viên", 
                true, com.datn.engflow.model.enums.LessonLevel.UPPER_INTERMEDIATE, 
                "https://api.dicebear.com/7.x/adventurer/svg?seed=admin");
    }

    /**
     * Inserts one demo user when the email is not taken.
     *
     * <p>An existing account is left completely untouched — the password is never
     * reset — so a user who changed the demo credentials keeps them across
     * restarts. The password is stored BCrypt-hashed through
     * {@link PasswordEncoder}.</p>
     *
     * @param email     unique login identifier
     * @param username  display handle
     * @param password  plaintext password, hashed before saving
     * @param fullName  display name
     * @param isAdmin   whether the account gets the ADMIN role
     * @param level     starting {@code LessonLevel} stored on the profile
     * @param avatarUrl remote avatar image URL
     */
    private void ensureUserExists(String email, String username, String password, String fullName,
                                  boolean isAdmin,
                                  com.datn.engflow.model.enums.LessonLevel level,
                                  String avatarUrl) {
        userRepository.findByEmail(email).ifPresentOrElse(
            user -> log.info("User {} already exists. Skipping (password not overwritten).", email),
            () -> {
                log.info("Default user {} not found. Creating it...", email);
                com.datn.engflow.model.entity.User user = com.datn.engflow.model.entity.User.builder()
                        .username(username)
                        .email(email)
                        .passwordHash(passwordEncoder.encode(password))
                        .fullName(fullName)
                        .avatarUrl(avatarUrl)
                        .isAdmin(isAdmin)
                        .currentLevel(level)
                        .totalPoints(0)
                        .isActive(true)
                        .build();
                userRepository.save(user);
            }
        );
    }
}
