package com.datn.engflow.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.model.enums.SkillType;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.service.HtmlParserService;
import com.datn.engflow.service.AnswerKeyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Import lesson JSON do crawler thu thập vào hai bảng {@code lessons} và
 * {@code exercises}.
 *
 * <p>Điều kiện bật: {@code @ConditionalOnProperty} với {@code matchIfMissing = true},
 * tức bean sẽ được tạo khi property {@code engflow.seed-json-data} VẮNG MẶT. Nhưng
 * {@code application.properties} đang set thẳng {@code engflow.seed-json-data=false},
 * nên mặc định hiện tại seeder KHÔNG chạy — đây là cố ý, đừng bật lại. Khi chạy,
 * nó idempotent: lesson chỉ được insert khi title chưa tồn tại, còn exercise vẫn
 * được parse cho cả lesson mới lẫn lesson đã có, nên restart có thể backfill
 * exercise mà không nhân đôi lesson. {@code @Order(2)} đặt nó sau
 * {@link DatabaseSeeder} để các bảng nền đã sẵn sàng (cùng order với
 * {@code VocabularyDataSeeder}). {@code JdbcTemplate} được inject nhưng chưa dùng
 * trong thân hiện tại — giữ lại cho đường bulk-insert của crawler.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
@ConditionalOnProperty(name = "engflow.seed-json-data", havingValue = "true", matchIfMissing = true)
public class JsonDataSeeder implements CommandLineRunner {

    private final LessonRepository lessonRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AnswerKeyService answerKeyService;
    private final HtmlParserService htmlParserService;
    private final ExerciseRepository exerciseRepository;

    /**
     * Đọc mọi file JSON của crawler và import các unit thành lesson.
     *
     * <p>Với mỗi unit, bảy key trong {@code skills} được map sang các hằng
     * {@link SkillType} theo đúng thứ tự, và mỗi {@code content} không rỗng được
     * làm sạch bằng Jsoup trước khi lưu. Các widget quen thuộc của scraper (quảng
     * cáo, shortcode, nút social) bị xoá và sau đó áp một safelist. Một exercise
     * chỉ được lưu khi chưa có exercise nào cùng lesson trùng question. Answer key
     * được load một lần ở cuối, lỗi chỉ log chứ không ném ra.</p>
     *
     * @param args tham số dòng lệnh truyền cho ứng dụng
     * @throws Exception nếu một file JSON không đọc hoặc parse được
     */
    @Override
    public void run(String... args) throws Exception {
        // Thử đọc từ thư mục crawler/data/ trước, nếu không có thì fallback về file đơn
        File dataDir = new File("crawler/data");
        File singleFile = new File("tienganh_nangcao_lessons.json");

        List<File> jsonFiles = new ArrayList<>();

        if (dataDir.exists() && dataDir.isDirectory()) {
            File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                jsonFiles.addAll(Arrays.asList(files));
            }
        } else if (singleFile.exists()) {
            jsonFiles.add(singleFile);
        }

        if (jsonFiles.isEmpty()) {
            log.info("No JSON data files found. Skipping seed.");
            return;
        }

        log.info("Starting JSON data seeding from {} file(s)", jsonFiles.size());

        final String[] skillKeys = {"vocabulary", "grammar", "listening", "reading", "writing", "speaking", "word_skills"};
        final SkillType[] skillTypes = {SkillType.VOCABULARY, SkillType.GRAMMAR, SkillType.LISTENING, SkillType.READING, SkillType.WRITING, SkillType.SPEAKING, SkillType.WORD_SKILLS};

        int orderIndex = 1;

        for (File jsonFile : jsonFiles) {
            log.info("Processing: {}", jsonFile.getName());
            JsonNode root = objectMapper.readTree(jsonFile);
            JsonNode units = root.get("units");

            String levelStr = root.path("level").asText("");
            LessonLevel lessonLevel = mapLevel(levelStr);
            String skillStr = root.path("skill").asText("");

            if (units == null || !units.isArray()) continue;

            for (JsonNode unit : units) {
                String title = unit.path("title").asText("Unit " + unit.path("unit").asText());
                if (title.length() < 5) continue; // Bỏ các entry rác

                JsonNode skillsNode = unit.path("skills");
                for (int i = 0; i < skillKeys.length; i++) {
                    JsonNode node = skillsNode.path(skillKeys[i]);
                    if (!node.isMissingNode()) {
                        String rawHtml = node.path("content").asText("");
                        if (!rawHtml.isEmpty()) {
                            Document doc = Jsoup.parse(rawHtml);
                            doc.select(".rpbt_shortcode").remove();
                            doc.select(".yarpp-related").remove();
                            doc.select("[class*=engli-ads]").remove();
                            doc.select(".engli-adlabel").remove();
                            doc.select(".et_social_bottom_trigger").remove();
                            doc.select(".post-meta").remove();
                            String cleanHtml = Jsoup.clean(doc.body().html(), Safelist.relaxed()
                                .addTags("audio", "source", "h5")
                                .addAttributes("audio", "src", "controls", "preload", "style", "class", "id")
                                .addAttributes("source", "src", "type")
                                .addAttributes("span", "style", "class", "onmouseout", "onmouseover", "media-url")
                                .addAttributes("div", "style", "class", "align")
                                .addAttributes("p", "style", "class", "align")
                                .addAttributes("img", "src", "width", "height", "class", "style")
                                .addAttributes("a", "href", "target", "class", "style")
                                .addAttributes("td", "width", "valign", "align", "colspan", "rowspan", "style")
                                .addAttributes("tr", "style", "class")
                                .addAttributes("table", "border", "cellpadding", "cellspacing", "width", "style", "class")
                                .addAttributes("strong", "class", "style")
                            );
                            
                            SkillType foundSkillType = skillTypes[i];
                            String fullTitle = title + " - " + foundSkillType.name();
                            if (lessonRepository.findByTitle(fullTitle).isEmpty()) {
                                Lesson lesson = Lesson.builder()
                                        .title(fullTitle)
                                        .description(fullTitle)
                                        .content(cleanHtml)
                                        .level(lessonLevel)
                                        .skillType(foundSkillType)
                                        .category(lessonLevel.name().replace("_", " "))
                                        .durationMinutes(30)
                                        .orderIndex(orderIndex++)
                                        .isPublished(true)
                                        .build();

                                lessonRepository.save(lesson);

                                // Parse exercise từ HTML content
                                try {
                                    List<Exercise> parsed = htmlParserService.parseExercises(lesson, cleanHtml);
                                    for (Exercise ex : parsed) {
                                        if (exerciseRepository.findByLessonIdOrderByOrderIndexAsc(lesson.getId())
                                                .stream().noneMatch(e -> e.getQuestion().equals(ex.getQuestion()))) {
                                            exerciseRepository.save(ex);
                                        }
                                    }
                                } catch (Exception ex) {
                                    log.warn("Could not parse exercises for lesson {}: {}", lesson.getId(), ex.getMessage());
                                }
                            } else {
                                log.info("Found existing lesson: {}. Parsing exercises...", fullTitle);
                                // Lesson đã tồn tại cũng được parse exercise
                                try {
                                    Lesson lesson = lessonRepository.findByTitle(fullTitle)
                                .orElseThrow(() -> new IllegalStateException("Lesson not found: " + fullTitle));
                                    if (lesson.getContent() != null && !lesson.getContent().isEmpty()) {
                                        List<Exercise> parsed = htmlParserService.parseExercises(lesson, lesson.getContent());
                                        for (Exercise ex : parsed) {
                                            if (exerciseRepository.findByLessonIdOrderByOrderIndexAsc(lesson.getId())
                                                    .stream().noneMatch(e -> e.getQuestion().equals(ex.getQuestion()))) {
                                                exerciseRepository.save(ex);
                                            }
                                        }
                                        log.info("  Parsed {} exercises for existing lesson: {}", parsed.size(), fullTitle);
                                    }
                                } catch (Exception ex) {
                                    log.warn("Could not parse exercises for existing lesson {}: {}", fullTitle, ex.getMessage());
                                }
                            }
                        }
                    }
                }
            }

            log.info("  Finished {}: {} lessons", jsonFile.getName(), units.size());
        }

        log.info("Finished JSON data seeding! Created {} total lessons.", orderIndex - 1);

        try {
            answerKeyService.loadAllAnswerKeys();
        } catch (Exception e) {
            log.warn("Could not load answer keys: {}", e.getMessage());
        }
    }

    /**
     * Map chuỗi level của crawler sang hằng {@link LessonLevel}.
     *
     * <p>Chấp nhận cả ký hiệu CEFR lẫn tên của enum. Mọi giá trị không nhận ra —
     * kể cả {@code null} và chuỗi rỗng — rơi về {@code ELEMENTARY} thay vì làm
     * fail cả lần import, vì một chuỗi level lạ trong corpus không đáng làm chết
     * quá trình seed.</p>
     *
     * @param level giá trị level thô từ JSON, có thể {@code null}
     * @return level khớp được, mặc định là {@code ELEMENTARY}
     */
    private LessonLevel mapLevel(String level) {
        if (level == null || level.isEmpty()) return LessonLevel.ELEMENTARY;
        return switch (level.toUpperCase()) {
            case "A1", "ELEMENTARY" -> LessonLevel.ELEMENTARY;
            case "A2", "PRE_INTERMEDIATE" -> LessonLevel.PRE_INTERMEDIATE;
            case "B1", "INTERMEDIATE" -> LessonLevel.INTERMEDIATE;
            case "B2", "UPPER_INTERMEDIATE" -> LessonLevel.UPPER_INTERMEDIATE;
            default -> LessonLevel.ELEMENTARY;
        };
    }
}
