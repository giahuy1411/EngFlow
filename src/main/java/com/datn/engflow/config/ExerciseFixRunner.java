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
 * Optional migration runner that rebuilds the {@code exercises} table from
 * lesson HTML.
 *
 * <p>Disabled by default and guarded by {@code engflow.exercise-fix.enabled};
 * when off, {@link #run} returns immediately. When on it is destructive — every
 * existing exercise row is deleted before the lessons are re-parsed by
 * {@link HtmlParserService}. {@code @Order(1)} runs it before the other
 * {@link org.springframework.boot.ApplicationRunner} beans so the rebuilt
 * exercises exist before anything seeds or reads them.</p>
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
     * Logs whether the destructive rebuild is armed.
     *
     * <p>Runs at bean construction so an operator can see the setting in the
     * startup log even when the migration is about to be skipped.</p>
     */
    @PostConstruct
    void init() {
        log.info("ExerciseFixRunner created, enabled={}", enabled);
    }

    /**
     * Deletes all exercises and re-parses them from every lesson's content.
     *
     * <p>Prefers {@code contentOriginal} over {@code content} so a lesson already
     * sanitized by {@code HtmlCleanupMigration} is rebuilt from the untouched
     * original. Lessons with neither field are counted as skipped. A parse failure
     * on one lesson is logged and recorded, then the loop continues, so one bad
     * lesson does not abort the migration.</p>
     *
     * @param args application startup arguments
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
