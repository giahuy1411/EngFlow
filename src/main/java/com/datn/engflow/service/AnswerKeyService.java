package com.datn.engflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnswerKeyService {

    private final Map<String, Map<Long, Map<Integer, String>>> keys = new HashMap<>();
    private static final Pattern ANSWER_PATTERN = Pattern.compile("result(\\d+)\\s*=\\s*['\"]?([^'\";,]+?)['\"]?\\s*[;,]");
    private static final Pattern ARR_RESULT_PATTERN = Pattern.compile("arr_result\\[\\d+]\\[\\d+]\\s*=\\s*['\"]?([^'\";]+?)['\"]?\\s*[;,]");

    @PostConstruct
    public void init() {
        loadFromJson();
    }

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
                System.err.println("AnswerKeyService: Could not find tienganh_nangcao_lessons.json");
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
            System.out.println("AnswerKeyService: Loaded answer keys for " + countKeys() + " quizzes");
        } catch (Exception e) {
            System.err.println("AnswerKeyService init error: " + e.getMessage());
        }
    }

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

    public void loadAllAnswerKeys() {
        loadFromJson();
    }

    public Map<Integer, String> getAnswers(long lessonId, String skillType) {
        Map<Long, Map<Integer, String>> byLesson = keys.get(skillType);
        if (byLesson == null) return Collections.emptyMap();
        return byLesson.getOrDefault(lessonId, Collections.emptyMap());
    }

    public boolean hasAnswers(long lessonId, String skillType) {
        return !getAnswers(lessonId, skillType).isEmpty();
    }

    private int countKeys() {
        return keys.values().stream()
            .mapToInt(m -> m.values().stream().mapToInt(Map::size).sum())
            .sum();
    }
}
