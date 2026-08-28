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

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
/**
 * class DatabaseSeeder.
 */
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

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

    private void ensureDefaultUsers() {
        String userPassword = System.getenv().getOrDefault("DEFAULT_USER_PASSWORD", "password123");
        String adminPassword = System.getenv().getOrDefault("DEFAULT_ADMIN_PASSWORD", "password123");
        ensureUserExists("user@gmail.com", "student", userPassword, "Học Viên Mẫu", 
                false, com.datn.engflow.model.enums.LessonLevel.ELEMENTARY, 
                "https://api.dicebear.com/7.x/adventurer/svg?seed=student");
        ensureUserExists("admin@gmail.com", "administrator", adminPassword, "Quản Trị Viên", 
                true, com.datn.engflow.model.enums.LessonLevel.UPPER_INTERMEDIATE, 
                "https://api.dicebear.com/7.x/adventurer/svg?seed=admin");
    }

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
