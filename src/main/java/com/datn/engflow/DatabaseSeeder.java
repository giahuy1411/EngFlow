package com.datn.engflow;

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
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
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
        
        // Ensure default users exist and have the correct BCrypt-encoded password "123456"
        ensureDefaultUsers();
    }

    private void ensureDefaultUsers() {
        ensureUserExists("user@gmail.com", "student", "123456", "Học Viên Mẫu", 
                com.datn.engflow.model.enums.UserRole.USER, com.datn.engflow.model.enums.LessonLevel.BEGINNER, 
                "https://api.dicebear.com/7.x/adventurer/svg?seed=student");
        ensureUserExists("admin@gmail.com", "administrator", "123456", "Quản Trị Viên", 
                com.datn.engflow.model.enums.UserRole.ADMIN, com.datn.engflow.model.enums.LessonLevel.ADVANCED, 
                "https://api.dicebear.com/7.x/adventurer/svg?seed=admin");
    }

    private void ensureUserExists(String email, String username, String password, String fullName, 
                                  com.datn.engflow.model.enums.UserRole role, 
                                  com.datn.engflow.model.enums.LessonLevel level, 
                                  String avatarUrl) {
        userRepository.findByEmail(email).ifPresentOrElse(
            user -> {
                log.info("User {} already exists. Updating password hash to guarantee it is correct.", email);
                user.setPasswordHash(passwordEncoder.encode(password));
                user.setUsername(username);
                user.setFullName(fullName);
                user.setRole(role);
                user.setCurrentLevel(level);
                user.setAvatarUrl(avatarUrl);
                userRepository.save(user);
            },
            () -> {
                log.info("Default user {} not found. Creating it...", email);
                com.datn.engflow.model.entity.User user = com.datn.engflow.model.entity.User.builder()
                        .username(username)
                        .email(email)
                        .passwordHash(passwordEncoder.encode(password))
                        .fullName(fullName)
                        .avatarUrl(avatarUrl)
                        .role(role)
                        .currentLevel(level)
                        .totalPoints(0)
                        .isActive(true)
                        .build();
                userRepository.save(user);
            }
        );
    }
}
