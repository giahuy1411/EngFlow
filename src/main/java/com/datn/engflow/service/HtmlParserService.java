package com.datn.engflow.service;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class HtmlParserService {

    private static final Pattern FILL_BLANK_PATTERN = Pattern.compile("…{3,}|_{3,}|\\.{3,}");

    /**
     * Parse HTML content from english-practice.net into Exercise objects.
     * HTML structure: each .et_pb_section is an exercise set.
     * Within each section:
     *   - .et_pb_toggle with title "Answer" contains answer key
     *   - <p> lines with <strong>Number</strong> are individual questions
     *   - <audio> or .et_pb_audio_module indicates LISTENING type
     */
    public List<Exercise> parseExercises(Lesson lesson, String htmlContent) {
        List<Exercise> exercises = new ArrayList<>();
        if (htmlContent == null || htmlContent.isEmpty()) return exercises;

        Document doc = Jsoup.parse(htmlContent);
        Elements sections = doc.select(".et_pb_section");

        if (sections.isEmpty()) {
            log.warn("No .et_pb_section found for lesson {}, trying direct parsing", lesson.getId());
            return parseFallback(lesson, doc);
        }

        int globalOrder = 1;
        for (Element section : sections) {
            ExerciseType type = detectExerciseType(section);

            // Extract answer key from toggle
            Element answerToggle = section.selectFirst(".et_pb_toggle:has(.et_pb_toggle_title:contains(Answer))");
            String answerContent = answerToggle != null ? answerToggle.select(".et_pb_toggle_content").html() : "";

            // Extract questions from <p> tags with <strong>Number</strong>
            Elements questionLines = section.select("p:has(strong)");
            for (Element line : questionLines) {
                String text = line.text().trim();
                if (text.length() < 3) continue;

                Element strong = line.selectFirst("strong");
                if (strong == null) continue;
                String numStr = strong.text().trim();
                int questionNum;
                try {
                    questionNum = Integer.parseInt(numStr.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                    continue;
                }

                String questionText = text.replaceFirst("^\\d+\\s*", "").trim();
                String correctAnswer = extractAnswerForQuestion(answerContent, questionNum);
                String options = extractOptions(section, questionNum);
                ExerciseDifficulty difficulty = detectDifficulty(section, lesson.getTitle());

                Exercise exercise = Exercise.builder()
                        .lesson(lesson)
                        .question(questionText)
                        .options(options)
                        .correctAnswer(correctAnswer)
                        .exerciseType(type)
                        .difficulty(difficulty)
                        .orderIndex(globalOrder++)
                        .build();
                exercises.add(exercise);
            }
        }

        log.info("Parsed {} exercises from lesson {}", exercises.size(), lesson.getId());
        return exercises;
    }

    private ExerciseType detectExerciseType(Element section) {
        if (!section.select("audio, .et_pb_audio_module").isEmpty()) {
            return ExerciseType.LISTENING;
        }
        if (!section.select(".et_pb_column:has(p:matches([a-f]))").isEmpty()) {
            return ExerciseType.MATCHING;
        }
        if (section.html().contains("…………") || section.html().contains("⋯⋯")) {
            return ExerciseType.FILL_BLANK;
        }
        return ExerciseType.FILL_BLANK;
    }

    private String extractAnswerForQuestion(String answerContent, int questionNum) {
        if (answerContent.isEmpty()) return "";
        Pattern p = Pattern.compile(
                "<strong>\\s*" + questionNum + "\\s*</strong>[\\s&nbsp;]*([^<]+)");
        java.util.regex.Matcher m = p.matcher(answerContent);
        if (m.find()) {
            return m.group(1).trim()
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("\\s+", " ");
        }
        return "";
    }

    private String extractOptions(Element section, int questionNum) {
        Elements columns = section.select(
                ".et_pb_column:has(p:matches([a-f]\\)))");
        if (columns.isEmpty()) return null;

        List<String> opts = new ArrayList<>();
        for (Element col : columns) {
            col.select("p").forEach(p -> {
                String t = p.text().trim();
                if (t.matches("^[a-f]\\)?\\s.*")) {
                    opts.add(t.replaceAll("^[a-f]\\)?\\s*", "").trim());
                }
            });
        }
        return opts.isEmpty() ? null : opts.toString();
    }

    private ExerciseDifficulty detectDifficulty(Element section, String title) {
        if (title.toUpperCase().contains("CHALLENGE") || section.html().contains("Challenge!")) {
            return ExerciseDifficulty.HARD;
        }
        return ExerciseDifficulty.MEDIUM;
    }

    private List<Exercise> parseFallback(Lesson lesson, Document doc) {
        List<Exercise> exercises = new ArrayList<>();
        Elements paragraphs = doc.select("p:has(strong)");
        int order = 1;
        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.length() < 5) continue;
            Exercise ex = Exercise.builder()
                    .lesson(lesson)
                    .question(text)
                    .correctAnswer("")
                    .exerciseType(ExerciseType.FILL_BLANK)
                    .difficulty(ExerciseDifficulty.MEDIUM)
                    .orderIndex(order++)
                    .build();
            exercises.add(ex);
        }
        return exercises;
    }
}
