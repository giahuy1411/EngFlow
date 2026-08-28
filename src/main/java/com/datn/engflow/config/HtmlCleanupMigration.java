package com.datn.engflow.config;

import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.service.LessonContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * One-time migration runner that deep-cleans all lesson HTML content.
 * Backs up original content to {@code content_original} column before cleaning.
 *
 * <p>Enabled by setting {@code engflow.html-cleanup.enabled=true} in application.properties.
 * After running successfully, set it back to {@code false} to prevent re-running.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "engflow.html-cleanup.enabled", havingValue = "true")
/**
 * class HtmlCleanupMigration.
 */
public class HtmlCleanupMigration implements ApplicationRunner {

    private final LessonRepository lessonRepository;
    private final LessonContentService lessonContentService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== HTML CLEANUP MIGRATION V2 STARTED ===");

        List<Lesson> lessons = lessonRepository.findAll();
        int total = lessons.size();
        int cleaned = 0;
        int skipped = 0;
        int errors = 0;
        long totalOriginalSize = 0;
        long totalCleanSize = 0;
        int batchSize = 50;
        List<Lesson> batch = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            Lesson lesson = lessons.get(i);
            try {
                // Use contentOriginal as source if available, fallback to content
                String source = lesson.getContentOriginal();
                if (source == null || source.isBlank()) {
                    source = lesson.getContent();
                }
                if (source == null || source.isBlank()) {
                    skipped++;
                    continue;
                }

                // Backup original content (only if not already backed up)
                if (lesson.getContentOriginal() == null) {
                    lesson.setContentOriginal(source);
                }

                // Deep clean from the best available source
                String cleanedContent = lessonContentService.deepCleanHtml(source);
                lesson.setContent(cleanedContent);

                totalOriginalSize += source.length();
                totalCleanSize += (cleanedContent != null ? cleanedContent.length() : 0);
                cleaned++;
                batch.add(lesson);

                // Batch save every N records to prevent losing all progress on abort
                if (batch.size() >= batchSize) {
                    lessonRepository.saveAll(batch);
                    batch.clear();
                    log.info("Batch saved at lesson {}/{} (cleaned: {}, skipped: {}, errors: {})",
                        i + 1, total, cleaned, skipped, errors);
                }
            } catch (Exception e) {
                errors++;
                log.error("Error cleaning lesson {} (id={}): {}", lesson.getTitle(), lesson.getId(), e.getMessage());
            }
        }

        // Save remaining batch
        if (!batch.isEmpty()) {
            lessonRepository.saveAll(batch);
        }

        double reduction = totalOriginalSize > 0
            ? (1.0 - (double) totalCleanSize / totalOriginalSize) * 100
            : 0;

        log.info("=== HTML CLEANUP MIGRATION V2 COMPLETED ===");
        log.info("Total: {} | Cleaned: {} | Skipped: {} | Errors: {}", total, cleaned, skipped, errors);
        log.info("Size reduction: {} chars -> {} chars ({}% reduction)",
            totalOriginalSize, totalCleanSize, String.format("%.1f", reduction));
        log.info(">>> IMPORTANT: Set engflow.html-cleanup.enabled=false to prevent re-running <<<");
    }
}
