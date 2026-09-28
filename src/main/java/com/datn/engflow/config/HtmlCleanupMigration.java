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
 * Runner migration chạy MỘT LẦN, dọn sâu toàn bộ HTML content của lesson.
 * Backup nội dung gốc vào cột {@code content_original} trước khi dọn.
 *
 * <p>Bật bằng {@code engflow.html-cleanup.enabled=true} trong application.properties
 * (mặc định {@code false}). Sau khi chạy xong phải set lại {@code false} để không
 * chạy lại — mỗi lần chạy đều ghi đè {@code content} của mọi lesson.</p>
 *
 * <p>Giống {@link ExerciseFixRunner} ở chỗ cùng ghi lại bảng {@code lessons}, nhưng
 * khác là bean chỉ được tạo khi property xuất hiện, nên cấu hình mặc định thậm chí
 * không khởi tạo runner. Việc dọn HTML được giao cho
 * {@link LessonContentService#deepCleanHtml(String)}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "engflow.html-cleanup.enabled", havingValue = "true")
public class HtmlCleanupMigration implements ApplicationRunner {

    private final LessonRepository lessonRepository;
    private final LessonContentService lessonContentService;

    /**
     * Dọn sâu content của mọi lesson, lưu theo batch.
     *
     * <p>Nguồn là {@code contentOriginal} nếu có, không thì lấy {@code content};
     * bản gốc chỉ được copy vào {@code contentOriginal} ở lần chạy đầu tiên, nên
     * lần chạy thứ hai vẫn giữ được text thật sự chưa bị đụng. Cứ 50 lesson lại
     * ghi ra một lần để nếu có abort thì tiến độ trước đó không mất. Dòng log
     * cuối báo tổng số ký tự giảm được.</p>
     *
     * @param args tham số khởi động của ứng dụng
     */
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
                // Lấy contentOriginal làm nguồn nếu có, không thì fallback về content
                String source = lesson.getContentOriginal();
                if (source == null || source.isBlank()) {
                    source = lesson.getContent();
                }
                if (source == null || source.isBlank()) {
                    skipped++;
                    continue;
                }

                // Backup nội dung gốc (chỉ khi chưa từng backup)
                if (lesson.getContentOriginal() == null) {
                    lesson.setContentOriginal(source);
                }

                // Dọn sâu từ nguồn tốt nhất đang có
                String cleanedContent = lessonContentService.deepCleanHtml(source);
                lesson.setContent(cleanedContent);

                totalOriginalSize += source.length();
                totalCleanSize += (cleanedContent != null ? cleanedContent.length() : 0);
                cleaned++;
                batch.add(lesson);

                // Lưu batch mỗi N record để abort không mất hết tiến độ
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

        // Lưu nốt batch còn lại
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
