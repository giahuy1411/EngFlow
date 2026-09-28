package com.datn.engflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
/**
 * Loads the answer keys of the imported {@code tienganh_nangcao_lessons.json}
 * corpus into memory at startup and serves them by lesson and skill code
 * ("vcb", "gra", "lis", "rea"). The keys are scraped out of the lesson HTML
 * with regexes rather than shipped as data, because the source file carries
 * answers only as visible text inside each skill's content. Read by the
 * data seeder; a corpus that is missing simply means no answers are available.
 */
public class AnswerKeyService {

    private static final Logger log = LoggerFactory.getLogger(AnswerKeyService.class);
    private final Map<String, Map<Long, Map<Integer, String>>> keys = new HashMap<>();
    private static final Pattern ANSWER_PATTERN = Pattern.compile("result(\\d+)\\s*=\\s*['\"]?([^'\";,]+?)['\"]?\\s*[;,]");
    private static final Pattern ARR_RESULT_PATTERN = Pattern.compile("arr_result\\[\\d+]\\[\\d+]\\s*=\\s*['\"]?([^'\";]+?)['\"]?\\s*[;,]");

    /**
     * Spring lifecycle hook: loads the corpus once, after the bean is
     * constructed but before it serves any request.
     */
    @PostConstruct
    public void init() {
        loadFromJson();
    }

    /**
     * Reads the corpus JSON and fills {@link #keys}. Tries the filesystem
     * first, then the classpath, so a development checkout can override the
     * bundled copy without a rebuild. Never throws: a missing or malformed
     * corpus is logged and leaves the service answering empty maps.
     */
    private void loadFromJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = null;
            String[] locations = {
                "tienganh_nangcao_lessons.json",
                "classpath:tienganh_nangcao_lessons.json"
            };
            for (String loc : locations) {
                try {
                    if (loc.startsWith("classpath:")) {
                        is = getClass().getClassLoader().getResourceAsStream(loc.substring(10));
                    } else {
                        is = new FileInputStream(loc);
                    }
                    if (is != null) break;
                } catch (Exception e) {
                    continue;
                }
            }
            if (is == null) {
                log.warn("AnswerKeyService: Could not find tienganh_nangcao_lessons.json");
                return;
            }

            JsonNode root = mapper.readTree(is);
            JsonNode units = root.get("units");
            if (units == null || !units.isArray()) return;

            String[] skillTypes = {"vocabulary", "grammar", "listening", "reading"};
            String[] skillCodes = {"vcb", "gra", "lis", "rea"};

            for (JsonNode unit : units) {
                long lessonId = unit.get("unit").asLong();
                JsonNode skills = unit.get("skills");
                if (skills == null) continue;

                for (int si = 0; si < skillTypes.length; si++) {
                    JsonNode skill = skills.get(skillTypes[si]);
                    if (skill == null) continue;
                    String content = skill.has("content") ? skill.get("content").asText() : "";
                    Map<Integer, String> answers = parseAnswers(content);
                    if (!answers.isEmpty()) {
                        keys.computeIfAbsent(skillCodes[si], k -> new HashMap<>())
                            .put(lessonId, answers);
                    }
                }
            }
            log.info("AnswerKeyService: Loaded answer keys for {} quizzes", countKeys());
        } catch (Exception e) {
            log.error("AnswerKeyService init error: {}", e.getMessage(), e);
        }
    }

    /**
     * Extracts the numbered answer map from one skill's HTML content. Two
     * shapes exist in the corpus: {@code resultN = 'x';} assignments and
     * {@code arr_result[i][j] = 'x';} entries, whose rows are numbered by
     * arrival order because the index does not match the visible question
     * number.
     *
     * @param html the skill's content HTML
     * @return question number to answer text, possibly empty
     */
    private Map<Integer, String> parseAnswers(String html) {
        Map<Integer, String> answers = new HashMap<>();
        if (html == null || html.isEmpty()) return answers;
        Matcher m = ANSWER_PATTERN.matcher(html);
        while (m.find()) {
            int qIdx = Integer.parseInt(m.group(1));
            String ans = m.group(2).trim();
            answers.put(qIdx, ans);
        }
        Matcher am = ARR_RESULT_PATTERN.matcher(html);
        while (am.find()) {
            String ans = am.group(1).trim();
            answers.put(answers.size(), ans);
        }
        return answers;
    }

    /**
     * Re-reads the corpus into memory, replacing whatever was loaded before.
     * Exposed so a long-running instance can pick up an edited corpus file
     * without a restart.
     */
    public void loadAllAnswerKeys() {
        loadFromJson();
    }

    /**
     * Looks up the answer key for one lesson and skill.
     *
     * @param lessonId  the lesson (unit) id
     * @param skillType the skill code: "vcb", "gra", "lis" or "rea"
     * @return question number to answer text; empty when unknown, never null
     */
    public Map<Integer, String> getAnswers(long lessonId, String skillType) {
        Map<Long, Map<Integer, String>> byLesson = keys.get(skillType);
        if (byLesson == null) return Collections.emptyMap();
        return byLesson.getOrDefault(lessonId, Collections.emptyMap());
    }

    /**
     * @param lessonId  the lesson (unit) id
     * @param skillType the skill code: "vcb", "gra", "lis" or "rea"
     * @return true when at least one answer was loaded for that pair
     */
    public boolean hasAnswers(long lessonId, String skillType) {
        return !getAnswers(lessonId, skillType).isEmpty();
    }

    /**
     * @return the total number of individual answers across every loaded skill,
     *         used only for the startup log line
     */
    private int countKeys() {
        return keys.values().stream()
            .mapToInt(m -> m.values().stream().mapToInt(Map::size).sum())
            .sum();
    }
}
