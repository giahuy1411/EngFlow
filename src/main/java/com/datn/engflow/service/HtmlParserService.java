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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
/**
 * class HtmlParserService.
 */
public class HtmlParserService {

    private static final Pattern FILL_BLANK_PATTERN = Pattern.compile("…{3,}|_{3,}|\\.{3,}");

    // Regex for option patterns: a) text, b) text, c. text, etc.
    private static final Pattern OPTION_PATTERN = 
        Pattern.compile("(?i)^\\s*[a-f][\\.\\)]\\s+");
    
    // Patterns for compact answer-key lines to filter out
    private static final Pattern ANSWER_KEY_NUMBERS = 
        Pattern.compile("^\\d+\\s+\\d+[\\s,;].*|^\\d+['’]\\s+\\w+.*");
    private static final Pattern ANSWER_KEY_LETTERS = 
        Pattern.compile("(?i)^[a-f]\\s+[a-z]\\s+.*|^[a-f]\\s+\\d+\\s+.*");
    
    // Pattern for detecting answer-key-only lines like "1 're 2 is 3 are"
    private static final Pattern ANSWER_KEY_CONTRACTIONS = 
        Pattern.compile("^\\d+\\s+['’][a-z]+\\s+\\d+\\s+['’][a-z]+.*");

    /**
     * Parse HTML content into Exercise objects.
     * Handles both raw Divi HTML (with .et_pb_section) and cleaned HTML 
     * (with native <details> for answers, or no wrapping sections).
     *
     * Strategy:
     * 1. Try extracting exercises from Divi .et_pb_section sections
     * 2. Try extracting from native <details> answer elements
     * 3. Try extracting from <p><strong>Number</strong> patterns directly
     * 4. Fallback to basic paragraph parsing
     */
    public List<Exercise> parseExercises(Lesson lesson, String htmlContent) {
        List<Exercise> exercises = new ArrayList<>();
        if (htmlContent == null || htmlContent.isEmpty()) return exercises;

        Document doc = Jsoup.parse(htmlContent);

        // Strategy 1: Divi sections (original scraped HTML)
        Elements sections = doc.select(".et_pb_section");
        if (!sections.isEmpty()) {
            return parseFromDiviSections(lesson, sections);
        }

        // Strategy 2: Cleaned HTML with native <details> for answers
        Element detailsEl = doc.selectFirst("details");
        if (detailsEl != null) {
            return parseFromDetailsHtml(lesson, doc);
        }

        // Strategy 3: Direct <p><strong>Number</strong> patterns
        Elements directParagraphs = doc.select("p:has(strong)");
        if (!directParagraphs.isEmpty()) {
            return parseFromParagraphs(lesson, doc);
        }

        log.warn("No parseable structure found for lesson {}", lesson.getId());
        return exercises;
    }

    // ===== STRATEGY 1: Parse from Divi .et_pb_section sections =====
    
    private List<Exercise> parseFromDiviSections(Lesson lesson, Elements sections) {
        List<Exercise> exercises = new ArrayList<>();
        int globalOrder = 1;

        for (Element section : sections) {
            ExerciseType type = detectDiviExerciseType(section);

            // Extract answer key from .et_pb_toggle (Divi accordion)
            // Handle both .et_pb_toggle_title and <h5>Answer</h5> formats
            Element answerToggle = section.selectFirst(
                ".et_pb_toggle:has(.et_pb_toggle_title:contains(Answer)), " +
                ".et_pb_toggle:has(h5:contains(Answer))");
            String answerContent = answerToggle != null 
                ? answerToggle.select(".et_pb_toggle_content").html() 
                : "";

            // Extract questions: <p> with <strong>Number>, EXCLUDE toggle content
            Elements allPLines = section.select("p:has(strong)");
            Elements questionLines = new Elements();
            for (Element p : allPLines) {
                if (isInsideToggle(p)) continue;
                if (isAnswerKeyLine(p)) continue;
                questionLines.add(p);
            }

            for (Element line : questionLines) {
                Exercise ex = buildExerciseFromP(line, lesson, type, answerContent, 
                    section, globalOrder++);
                if (ex != null) {
                    exercises.add(ex);
                }
            }
        }

        log.info("Strategy 1 (Divi): lesson {} → {} exercises", lesson.getId(), exercises.size());
        return exercises;
    }

    private boolean isInsideToggle(Element el) {
        for (Element parent : el.parents()) {
            if (parent.hasClass("et_pb_toggle_content") || 
                parent.hasClass("et_pb_toggle") ||
                parent.hasClass("et_pb_toggle_title")) {
                return true;
            }
        }
        return false;
    }

    private boolean isAnswerKeyLine(Element el) {
        String text = el.text().trim();
        // Remove leading number
        text = text.replaceFirst("^\\d+[\\s\\.\\)]*", "").trim();
        // Check if remaining text is just answer key fragments
        if (text.matches("^[''']?[a-zA-Z0-9]+\\s+\\d+.*") && text.length() > 6) return true;
        if (text.matches("^\\d+\\s+[''']?[a-zA-Z]+.*") && text.length() > 6) return true;
        return false;
    }

    private ExerciseType detectDiviExerciseType(Element section) {
        // 1. Audio present → LISTENING
        if (!section.select("audio, .et_pb_audio_module").isEmpty()) {
            return ExerciseType.LISTENING;
        }
        // 2. Column with letter-number options → MATCHING
        if (!section.select(".et_pb_column:has(p:matches([a-f]))").isEmpty()) {
            return ExerciseType.MATCHING;
        }
        // 3. Ellipsis/underline patterns → FILL_BLANK
        if (section.html().contains("…………") || section.html().contains("⋯⋯")) {
            return ExerciseType.FILL_BLANK;
        }
        // 4. Has letter-based options (a), b), c) etc.) → MULTIPLE_CHOICE
        if (hasOptionPatterns(section)) {
            return ExerciseType.MULTIPLE_CHOICE;
        }
        // Default: MULTIPLE_CHOICE (most common type on english-practice.net)
        return ExerciseType.MULTIPLE_CHOICE;
    }

    // ===== STRATEGY 2: Parse from cleaned HTML (native <details> answers) =====

    private List<Exercise> parseFromDetailsHtml(Lesson lesson, Document doc) {
        List<Exercise> exercises = new ArrayList<>();
        
        // Collect all answer content from <details><summary>Answer</summary>...</details>
        String allAnswerContent = "";
        Element detailsAnswer = doc.selectFirst("details > summary:contains(Answer)");
        if (detailsAnswer != null) {
            Element detailsParent = detailsAnswer.parent();
            if (detailsParent != null) {
                allAnswerContent = detailsParent.html();
            }
        }

        // Split content into logical exercise sections (by <br><br>, <p>, <hr>, etc.)
        Elements paragraphs = doc.select("p:has(strong)");
        int order = 1;
        
        for (Element p : paragraphs) {
            Exercise ex = buildExerciseFromP(p, lesson, null, allAnswerContent, null, order++);
            if (ex != null) {
                exercises.add(ex);
            }
        }

        log.info("Strategy 2 (details): lesson {} → {} exercises", lesson.getId(), exercises.size());
        return exercises;
    }

    // ===== STRATEGY 3: Parse from direct paragraphs =====

    private List<Exercise> parseFromParagraphs(Lesson lesson, Document doc) {
        List<Exercise> exercises = new ArrayList<>();
        
        // Try to find answer content in any <details> element
        String allAnswerContent = "";
        Element detailsAnswer = doc.selectFirst("details > summary:contains(Answer)");
        if (detailsAnswer != null) {
            Element detailsParent = detailsAnswer.parent();
            if (detailsParent != null) {
                allAnswerContent = detailsParent.html();
            }
        }

        // Also look for inline answer patterns like "Answer:"
        Element answerSection = doc.selectFirst("p:contains(Answer), strong:contains(Answer)");
        if (answerSection != null && allAnswerContent.isEmpty()) {
            Element sibling = answerSection.nextElementSibling();
            if (sibling != null) {
                allAnswerContent = sibling.html();
            }
        }

        // Detect overall exercise type for the lesson
        ExerciseType lessonType = detectLessonType(doc);
        
        Elements paragraphs = doc.select("p:has(strong)")
            .not("details p"); // exclude paragraphs inside <details>
        
        int order = 1;
        for (Element p : paragraphs) {
            Exercise ex = buildExerciseFromP(p, lesson, lessonType, allAnswerContent, null, order++);
            if (ex != null) {
                exercises.add(ex);
            }
        }

        log.info("Strategy 3 (paragraphs): lesson {} → {} exercises", lesson.getId(), exercises.size());
        return exercises;
    }

    // ===== SHARED BUILDING BLOCKS =====

    /**
     * Build Exercise from a <p><strong>Number</strong> element.
     * Returns null if the paragraph is not a valid exercise question.
     */
    private Exercise buildExerciseFromP(Element p, Lesson lesson, ExerciseType typeHint,
                                         String answerContent, Element sectionContext, int order) {
        String text = p.text().trim();
        if (text.length() < 3) return null;

        // Extract question number from <strong>
        Element strong = p.selectFirst("strong");
        if (strong == null) return null;
        
        int questionNum;
        try {
            questionNum = Integer.parseInt(strong.text().trim().replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return null;
        }

        // Remove the leading number to get question text
        String questionText = text.replaceFirst("^\\d+[\\s\\.\\)]*", "").trim();

        // Skip if it's clearly an answer-key reference line
        if (isAnswerReferenceLine(questionText)) return null;

        // Skip empty questions
        if (questionText.isEmpty() || questionText.length() < 2) return null;

        // Detect type for this specific exercise
        ExerciseType type = typeHint != null ? typeHint : detectExerciseTypeFromText(questionText, p, sectionContext);

        // Extract answer for this question
        String correctAnswer = extractAnswerForQuestion(answerContent, questionNum);

        // Extract options for multiple choice
        String options = extractOptions(p, sectionContext, questionNum);

        ExerciseDifficulty difficulty = sectionContext != null 
            ? detectDifficulty(sectionContext, lesson.getTitle())
            : detectDifficulty(lesson.getTitle());

        return Exercise.builder()
                .lesson(lesson)
                .question(questionText)
                .options(options)
                .correctAnswer(correctAnswer)
                .exerciseType(type)
                .difficulty(difficulty)
                .orderIndex(order)
                .build();
    }

    /**
     * Check if a text line is a compact answer key (not a real question).
     */
    private boolean isAnswerReferenceLine(String text) {
        if (text.isEmpty()) return true;
        
        // "1 're 2 is 3 are 4 doing 5 does" — compact grammar answers
        if (ANSWER_KEY_CONTRACTIONS.matcher(text).matches()) return true;
        
        // "1 2 3 4 5" — just numbers
        if (text.matches("^\\d+[\\s,;&/-]*\\d+.*") && 
            text.replaceAll("[\\d,\\s;&/-]", "").length() <= 2) return true;
        
        // "1  2  3  4  5" with spacing
        if (text.matches("^\\d+[\\s]{2,}\\d+.*")) return true;
        
        // "F 2 T 3 F 4 T" — true/false answer keys
        if (text.matches("(?i)^[ft]\\s+\\d+\\s+[ft]\\s+\\d+.*")) return true;
        
        // "a 2 b 3 c 4" — matching answer keys
        if (text.matches("(?i)^[a-f]\\s+\\d+\\s+[a-f]\\s+\\d+.*")) return true;

        // "1-a 2-b 3-c" type answer keys
        if (text.matches("^\\d+\\s*[-–]\\s*[a-fA-F].*\\d+\\s*[-–]\\s*[a-fA-F].*")) return true;

        return false;
    }

    /**
     * Detect exercise type from the question text and context.
     */
    private ExerciseType detectExerciseTypeFromText(String questionText, Element p, Element sectionContext) {
        // Check for fill-blank patterns
        if (FILL_BLANK_PATTERN.matcher(questionText).find()) {
            return ExerciseType.FILL_BLANK;
        }

        // Check if this paragraph has child elements with options (a), b), c) etc.)
        if (hasOptionPatterns(p)) {
            return ExerciseType.MULTIPLE_CHOICE;
        }

        // If section context has options anywhere, assume MULTIPLE_CHOICE
        if (sectionContext != null && hasOptionPatterns(sectionContext)) {
            return ExerciseType.MULTIPLE_CHOICE;
        }

        // Check if text contains option-like patterns inline (e.g. "a. Hanoi  b. Paris")
        if (questionText.matches(".*[a-f][\\.\\)]\\s+[A-Z].*")) {
            return ExerciseType.MULTIPLE_CHOICE;
        }

        // Default to MULTIPLE_CHOICE (most common type)
        return ExerciseType.MULTIPLE_CHOICE;
    }

    /**
     * Check if an element (or its children) contains option patterns like a), b), c).
     */
    private boolean hasOptionPatterns(Element el) {
        String html = el.html();
        // Check for lettered option patterns: a) text, b. text, (a) text, etc.
        return html.matches(".*[a-f][\\.\\)]\\s+[\\w].*") || 
               html.matches(".*\\([a-f]\\)\\s+[\\w].*") ||
               !el.select("p:matches([a-f]\\)), li:matches([a-f]\\))").isEmpty();
    }

    /**
     * Extract answer for a specific question number from answer content.
     * Handles both raw Divi toggle HTML and cleaned <details> HTML.
     */
    private String extractAnswerForQuestion(String answerContent, int questionNum) {
        if (answerContent == null || answerContent.isBlank()) return "";

        // Normalize HTML - handle Unicode apostrophes and entities
        String normalized = answerContent
            .replace("\u2019", "'")   // Right single quotation mark ’
            .replace("\u2018", "'")   // Left single quotation mark '
            .replace("\u201c", "\"")  // Left double "
            .replace("\u201d", "\"")  // Right double "
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("</p>", "\n")
            .replaceAll("<[^>]+>", " ")
            .replaceAll("&[a-z]+;", " ")
            .replaceAll("\\s+", " ")
            .trim();

        // Try inline pattern: "1 're 2 is 3 are" (number answer number answer, no separators)
        Pattern inlinePattern = Pattern.compile(
            "\\b" + questionNum + "\\s+([^\\d]+?)(?=\\s+\\d+\\s|\\z)");
        Matcher inlineMatcher = inlinePattern.matcher(normalized + " ");
        if (inlineMatcher.find()) {
            String answer = inlineMatcher.group(1).trim();
            if (answer.length() > 0 && answer.length() < 100) {
                return answer.replaceAll(" {2,}", " ").trim();
            }
        }

        // Try conventional patterns: "1. answer" or "1) answer" or "1: answer"
        List<String> patterns = Arrays.asList(
            "\\b" + questionNum + "\\s*[\\.\\):]\\s*(.+?)(?=\\s*\\d+\\s*[\\.\\):]|$)",
            "\\b" + questionNum + "\\s+[-–]\\s+(.+?)(?=\\s*\\d+\\s+[-–]\\s+|$)",
            "\\b" + questionNum + "\\s{2,}(.+?)(?=\\s{2,}\\d+|$)"
        );

        for (String patternStr : patterns) {
            Pattern p = Pattern.compile(patternStr, Pattern.DOTALL);
            Matcher m = p.matcher(normalized + " ");
            if (m.find()) {
                String answer = m.group(1).trim();
                if (answer.length() > 1 && answer.length() < 200) {
                    return answer;
                }
            }
        }

        // Fallback: match <strong>Q</strong> in raw HTML, capture answer text including unicode
        // Handles: <strong>1</strong>’re&nbsp;&nbsp;<strong>2</strong>
        // Capture everything after </strong> up to next <strong> or end
        String rawWithUnicode = answerContent
            .replace("\u2019", "'")
            .replace("\u2018", "'");
        Pattern htmlFallback = Pattern.compile(
            "<strong>\\s*" + questionNum + "\\s*</strong>\\s*([^<]+?)(?=\\s*<strong>|\\s*&nbsp;\\s*<strong>|\\s*$)");
        Matcher htmlMatcher = htmlFallback.matcher(rawWithUnicode);
        if (htmlMatcher.find()) {
            String answer = htmlMatcher.group(1).trim()
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("\"", "");  // Strip double quotes, keep apostrophe
            if (answer.length() > 0 && answer.length() < 200) {
                return answer;
            }
        }

        return "";
    }

    /**
     * Extract multiple choice options for a question.
     * Handles both Divi column layout and inline option patterns.
     */
    private String extractOptions(Element p, Element sectionContext, int questionNum) {
        List<String> opts = new ArrayList<>();

        // Try extracting from the paragraph's following siblings (options in next lines)
        if (p != null) {
            Element next = p.nextElementSibling();
            int maxSiblings = 5;
            while (next != null && maxSiblings-- > 0) {
                String t = next.text().trim();
                if (t.matches("^[a-f][\\.\\)]\\s.*")) {
                    opts.add(t.replaceAll("^[a-f][\\.\\)]\\s*", "").trim());
                } else if (!opts.isEmpty()) {
                    break; // stop if we already found options and this line doesn't match
                }
                next = next.nextElementSibling();
            }
        }

        // If options found in siblings, return them
        if (!opts.isEmpty()) {
            return formatOptions(opts);
        }

        // Try extracting from Divi columns
        if (sectionContext != null) {
            Elements columns = sectionContext.select(
                ".et_pb_column:has(p:matches([a-f]\\)))");
            for (Element col : columns) {
                col.select("p").forEach(colP -> {
                    String t = colP.text().trim();
                    if (t.matches("^[a-f][\\.\\)]?\\s.*")) {
                        opts.add(t.replaceAll("^[a-f][\\.\\)]?\\s*", "").trim());
                    }
                });
            }
        }

        if (opts.isEmpty()) return null;
        return formatOptions(opts);
    }

    private String formatOptions(List<String> options) {
        if (options == null || options.isEmpty()) return null;
        // Return as JSON-like array string for storage
        return "[\"" + options.stream()
            .map(o -> o.replace("\"", "\\\""))
            .collect(Collectors.joining("\", \"")) + "\"]";
    }

    /**
     * Detect lesson-level exercise type from document content.
     */
    private ExerciseType detectLessonType(Document doc) {
        String html = doc.html();
        
        // Audio present
        if (!doc.select("audio").isEmpty()) return ExerciseType.LISTENING;
        
        // Fill-blank patterns
        if (html.contains("……") || html.contains("⋯⋯")) return ExerciseType.FILL_BLANK;
        
        // Option patterns throughout
        if (hasOptionPatterns(doc)) return ExerciseType.MULTIPLE_CHOICE;
        
        // Check for ellipsis patterns
        if (FILL_BLANK_PATTERN.matcher(html).find()) return ExerciseType.FILL_BLANK;
        
        return ExerciseType.MULTIPLE_CHOICE;
    }

    /**
     * Extract answer from arrow/slash pattern:
     * "word / word → answer" or "This coffee / hot => is"
     */
    private String extractByArrowPattern(String normalized, int questionNum) {
        String prefix = questionNum + ".";
        int idx = normalized.indexOf(prefix);
        if (idx < 0) {
            prefix = questionNum + ")";
            idx = normalized.indexOf(prefix);
        }
        if (idx < 0) return null;

        String afterNum = normalized.substring(idx + prefix.length()).trim();
        // Look for → or => or / patterns
        Matcher arrowMatcher = Pattern.compile("([^/→=>]+?)\s*(?:→|=>|/)\s*(.+?)(?=\s*\\d+[.)]|$)").matcher(afterNum);
        if (arrowMatcher.find()) {
            String answer = arrowMatcher.group(2).trim();
            if (!answer.isEmpty() && answer.length() < 100) {
                return cleanAnswerText(answer);
            }
        }
        return null;
    }

    /**
     * Extract from bullet list: "• answer" or "- answer"
     */
    private String extractByBulletPattern(String normalized, int questionNum) {
        String prefix = questionNum + ".";
        int idx = normalized.indexOf(prefix);
        if (idx < 0) {
            prefix = questionNum + ")";
            idx = normalized.indexOf(prefix);
        }
        if (idx < 0) return null;

        String afterNum = normalized.substring(idx + prefix.length()).trim();
        // Look for bullet content: a) answer or  -  answer or * answer
        Matcher bulletMatcher = Pattern.compile("\\b[\\w\\s]+(?=\\s*[\\-\\*•]\\s+|$)|([\\-\\*•])\\s*(.+)").matcher(afterNum);
        if (bulletMatcher.find()) {
            String answer = bulletMatcher.group(2);
            if (answer != null) {
                answer = answer.trim();
                if (!answer.isEmpty() && answer.length() < 100) {
                    return cleanAnswerText(answer);
                }
            }
        }
        return null;
    }

    private String cleanAnswerText(String text) {
        if (text == null) return "";
        return text.replaceAll("^[\"']+|[\"']+$", "").trim();
    }

    private ExerciseDifficulty detectDifficulty(Element section, String title) {
        if (title.toUpperCase().contains("CHALLENGE") || 
            (section != null && section.html().contains("Challenge!"))) {
            return ExerciseDifficulty.HARD;
        }
        return ExerciseDifficulty.MEDIUM;
    }

    private ExerciseDifficulty detectDifficulty(String title) {
        if (title != null && title.toUpperCase().contains("CHALLENGE")) {
            return ExerciseDifficulty.HARD;
        }
        return ExerciseDifficulty.MEDIUM;
    }
}
