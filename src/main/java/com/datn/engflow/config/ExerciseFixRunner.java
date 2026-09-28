package com.datn.engflow.config;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.service.HtmlParserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner migration tuỳ chọn, dựng lại bảng {@code exercises} từ HTML của lesson.
 *
 * <p>Mặc định TẮT và được canh bởi {@code engflow.exercise-fix.enabled}; khi off,
 * {@link #run} return ngay. Khi bật thì đây là thao tác PHÁ HUỶ — mọi row exercise
 * hiện có bị xoá trước khi lesson được parse lại bởi {@link HtmlParserService}.
 * {@code @Order(1)} cho nó chạy trước các bean
 * {@link org.springframework.boot.ApplicationRunner} khác, nhờ vậy exercise dựng
 * lại đã tồn tại trước khi có thứ gì seed hoặc đọc chúng.</p>
 *
 * <p>Trong {@code application.properties} cờ này cố ý để {@code false}: đây là
 * runner chạy MỘT LẦN cho đợt sửa dữ liệu, KHÔNG bật lại — mỗi lần chạy đều xoá
 * sạch exercise rồi parse lại toàn bộ.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class ExerciseFixRunner implements ApplicationRunner {

    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final HtmlParserService htmlParserService;

    @Value("${engflow.exercise-fix.enabled:false}")
    private boolean enabled;

    /**
     * Ghi log cờ cấu hình của bước dựng lại exercise.
     *
     * <p>Chạy ngay lúc khởi tạo bean để operator thấy được setting trong log
     * startup, kể cả khi migration sắp bị bỏ qua.</p>
     */
    @PostConstruct
    void init() {
        log.info("ExerciseFixRunner created, enabled={}", enabled);
    }

    /**
     * Xoá toàn bộ exercise rồi parse lại từ content của từng lesson.
     *
     * <p>Ưu tiên {@code contentOriginal} hơn {@code content}, để lesson đã bị
     * {@code HtmlCleanupMigration} làm sạch vẫn được dựng lại từ bản gốc chưa
     * đụng tới. Lesson không có cả hai field được tính là skipped. Lỗi parse ở
     * một lesson chỉ được log và ghi nhận rồi vòng lặp đi tiếp, nên một lesson
     * hỏng không làm chết cả migration.</p>
     *
     * @param args tham số khởi động của ứng dụng
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("ExerciseFixRunner.run() called, enabled={}", enabled);
        if (!enabled) {
            log.info("ExerciseFixRunner: SKIPPED (engflow.exercise-fix.enabled=false)");
            return;
        }

        log.info("=== EXERCISE FIX MIGRATION STARTED ===");
        log.info("Step 1: Deleting all existing exercises...");

        long beforeCount = exerciseRepository.count();
        exerciseRepository.deleteAllInBatch();
        log.info("Deleted {} exercises", beforeCount);

        log.info("Step 2: Re-seeding exercises from lesson content...");

        List<Lesson> allLessons = lessonRepository.findAll();
        int totalParsed = 0;
        int totalSkipped = 0;
        int totalErrors = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < allLessons.size(); i++) {
            Lesson lesson = allLessons.get(i);
            try {
                String content = lesson.getContentOriginal();
                if (content == null || content.isBlank()) {
                    content = lesson.getContent();
                }

                if (content == null || content.isBlank()) {
                    totalSkipped++;
                    continue;
                }

                List<Exercise> exercises = htmlParserService.parseExercises(lesson, content);
                if (!exercises.isEmpty()) {
                    exerciseRepository.saveAll(exercises);
                    totalParsed += exercises.size();
                } else {
                    totalSkipped++;
                }

                if ((i + 1) % 100 == 0 || (i + 1) == allLessons.size()) {
                    log.info("Progress: {}/{} lessons | Parsed: {} exercises | Skipped: {} | Errors: {}",
                        i + 1, allLessons.size(), totalParsed, totalSkipped, totalErrors);
                }
            } catch (Exception e) {
                totalErrors++;
                String msg = "Lesson " + lesson.getId() + " (" + lesson.getTitle() + "): " + e.getMessage();
                errors.add(msg);
                log.error("Error processing lesson {}: {}", lesson.getId(), e.getMessage());
            }
        }

        log.info("=== EXERCISE FIX MIGRATION COMPLETED ===");
        log.info("Lessons processed: {} | Exercises generated: {} | Skipped: {} | Errors: {}",
            allLessons.size(), totalParsed, totalSkipped, totalErrors);
        if (!errors.isEmpty()) {
            log.warn("Errors encountered:");
            errors.forEach(e -> log.warn("  - {}", e));
        }
        log.info(">>> IMPORTANT: Set engflow.exercise-fix.enabled=false to prevent re-running <<<");
    }
}
