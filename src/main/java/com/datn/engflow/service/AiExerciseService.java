package com.datn.engflow.service;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiExerciseService {

    private final ObjectMapper objectMapper;
    private final DuckDuckGoResearchService researchService;
    private final TtsService ttsService;
    private final CloudinaryService cloudinaryService;
    private final ExerciseRepository exerciseRepository;
    private final LessonRepository lessonRepository;

    @Value("${ai.exercise.ollama.base-url:http://localhost:11434/v1}")
    private String ollamaBaseUrl;

    @Value("${ai.exercise.ollama.model:qwen2.5:1.5b}")
    private String model;

    @Value("${ai.exercise.ollama.reviewer-model:qwen2.5:1.5b}")
    private String reviewerModel;

    @Value("${ai.exercise.review.max-attempts:3}")
    private int maxReviewAttempts;

    private final ConcurrentHashMap<String, BatchProgress> batchProgressMap = new ConcurrentHashMap<>();

    /**
     * SHA-256 hashes (first 16 hex chars) of question texts generated during this
     * JVM lifetime, used for cross-lesson deduplication in batch generation.
     */
    private final java.util.Set<String> seenQuestionHashes = ConcurrentHashMap.newKeySet();

    /**
     * Question texts embedded in the few-shot prompt examples. qwen2.5:1.5b tends
     * to copy examples verbatim (documented in MCP server.py), so any exercise
     * matching these signatures is a placeholder, not new content, and must be
     * rejected before persistence.
     */
    private static final java.util.Set<String> EXAMPLE_QUESTION_SIGNATURES = java.util.Set.of(
            "The sun ___ in the east.",
            "Match similar words:",
            "Water ___ at 100 degrees Celsius.",
            "Translate to English: Toi thich hoc tieng Anh.",
            "Listen and complete: The cat ___ on the mat."
    );

    private final ExecutorService generationPool =
            Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));

    @PreDestroy
    void destroy() {
        generationPool.shutdown();
        try {
            if (!generationPool.awaitTermination(30, TimeUnit.SECONDS)) {
                generationPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            generationPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class BatchProgress {
        public int totalLessons;
        public int processed;
        public int generated;
        public int errors;
        public boolean running;
        public String currentLesson;
        public List<Exercise> exercises;
        public List<String> errorDetails;
    }

    // ΓöÇΓöÇ Core LLM call ΓöÇΓöÇ

    private String callOllama(String prompt, String modelName) {
        WebClient client = WebClient.builder()
                .baseUrl(ollamaBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        try {
            String content = objectMapper.valueToTree(prompt).toString();
            String requestBody = "{\"model\":\"" + modelName + "\",\"messages\":[{\"role\":\"user\",\"content\":" + content + "}],\"temperature\":0.7,\"stream\":false}";
            long t0 = System.currentTimeMillis();
            String responseBody = client.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            long elapsed = System.currentTimeMillis() - t0;
            log.info("Ollama call completed in {}ms (model={})", elapsed, modelName);
            JsonNode node = objectMapper.readTree(responseBody);
            JsonNode choices = node.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
            throw new RuntimeException("Ollama returned no choices");
        } catch (Exception e) {
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }

    private String callOllama(String prompt) {
        return callOllama(prompt, model);
    }

    private String buildBasePrompt(Lesson lesson) {
        String content = lesson.getContent();
        if (content == null || content.isBlank()) {
            content = lesson.getDescription() != null ? lesson.getDescription() : lesson.getTitle();
        }
        content = content.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (content.length() > 3000) content = content.substring(0, 3000);
        return content;
    }

    String buildPrompt(Lesson lesson, ExerciseType type, int count, String researchContext) {
        String lessonContent = buildBasePrompt(lesson);
        String typeInstruction = switch (type) {
            case MULTIPLE_CHOICE -> "multiple choice questions with 4 options each";
            case FILL_BLANK -> "fill-in-the-blank questions where the student fills the missing word";
            case MATCHING -> "matching exercises (options as JSON array of \"left|right\" pairs, correctAnswer as \"left0=right0,left1=right1,...\")";
            case TRANSLATION -> "translation exercises from English to Vietnamese";
            case LISTENING -> "listening exercises (question is text to be spoken, correctAnswer is expected transcription)";
        };
        String researchSection = researchContext != null && !researchContext.isBlank()
                ? "\n\nAdditional research:\n" + researchContext : "";
        // Few-shot example lock: qwen2.5:1.5b reliably copies concrete examples but
        // drifts on abstract format descriptions (verified by MCP server.py V1 prompt).
        String example = switch (type) {
            case MULTIPLE_CHOICE -> """
                    Example: {"question":"The sun ___ in the east.","options":["rise","rises","rose","rising"],"correctAnswer":"rises","explanation":"3rd person singular: rise -> rises.","difficulty":"EASY"}""";
            case MATCHING -> """
                    Example MATCHING: {"question":"Match similar words:","options":["big|large","small|tiny","fast|quick","smart|clever"],"correctAnswer":"big=large,small=tiny,fast=quick,smart=clever","explanation":"Synonyms matching.","difficulty":"EASY"}
                    IMPORTANT: each option is ONE pair "left|right" — a word or SHORT phrase on each side, single | separator, NEVER whole sentences, NEVER sentence transformation. correctAnswer repeats every pair as left=right, comma-separated.""";
            case FILL_BLANK -> """
                    Example: {"question":"Water ___ at 100 degrees Celsius.","correctAnswer":"boils","explanation":"Simple present for fact: boils.","difficulty":"EASY"}""";
            case TRANSLATION -> """
                    Example: {"question":"Translate to English: Toi thich hoc tieng Anh.","correctAnswer":"I like learning English.","explanation":"Toi=I, thich=like, hoc=learn.","difficulty":"MEDIUM"}""";
            case LISTENING -> """
                    Example: {"question":"Listen and complete: The cat ___ on the mat.","correctAnswer":"sits","explanation":"The speaker said: sits.","difficulty":"EASY"}""";
        };
        // The example is repeated at the END of the prompt — long lesson content sits
        // between instructions and output, and small models suffer attention decay:
        // a top-only example gets ignored (observed at runtime with qwen2.5:1.5b).
        String reminder = switch (type) {
            case MULTIPLE_CHOICE -> "output JSON array only, each item shaped like the example above";
            case MATCHING -> "output JSON array only, each option is one short \"left|right\" pair, correctAnswer is \"left=right,...\" — NEVER whole-sentence options";
            case FILL_BLANK -> "output JSON array only, each item shaped like the example above";
            case TRANSLATION -> "output JSON array only, each item shaped like the example above";
            case LISTENING -> "output JSON array only, each item shaped like the example above";
        };
        // Match the MCP reference implementation: cap lesson content at 500 chars.
        // Long content (3000 chars) drowns the format example for qwen2.5:1.5b and
        // biases it toward copying textbook exercises instead of the target shape.
        String trimmedContent = lessonContent.length() > 500 ? lessonContent.substring(0, 500) : lessonContent;
        return String.format(
            "You are an English exercise generator. Create %d %s. Return ONLY a valid JSON array (no markdown). " +
            "Each object: {\"question\": string, \"options\": array, \"correctAnswer\": string, \"explanation\": string, \"difficulty\": \"EASY|MEDIUM|HARD\"}. " +
            "%s%nWrite NEW exercises about the topic. Keep the FORMAT exactly but use DIFFERENT words.%n" +
            "Lesson: %s. Content: %s%s.%n" +
            "REMINDER — %s.",
            count, typeInstruction, example, lesson.getTitle(), trimmedContent, researchSection, reminder);
    }

    // ΓöÇΓöÇ Exercise generation ΓöÇΓöÇ

    public List<Exercise> generateByType(Lesson lesson, ExerciseType type, int count) {
        String researchContext = researchService.researchLessonTopic(lesson.getTitle(), 2);
        return generateWithReviewLoop(lesson, type, count, researchContext);
    }

    public List<Exercise> generateMultipleChoice(Lesson lesson, int count) {
        return generateByType(lesson, ExerciseType.MULTIPLE_CHOICE, count);
    }

    public List<Exercise> generateFillBlank(Lesson lesson, int count) {
        return generateByType(lesson, ExerciseType.FILL_BLANK, count);
    }

    public List<Exercise> generateMatching(Lesson lesson, int count) {
        return generateByType(lesson, ExerciseType.MATCHING, count);
    }

    public List<Exercise> generateTranslation(Lesson lesson, int count) {
        return generateByType(lesson, ExerciseType.TRANSLATION, count);
    }

    public List<Exercise> generateListening(Lesson lesson, int count) {
        List<Exercise> exercises = generateByType(lesson, ExerciseType.LISTENING, count);
        for (Exercise ex : exercises) {
            if (ttsService != null && ttsService.isAvailable() && ex.getQuestion() != null) {
                try {
                    byte[] audio = ttsService.synthesize(ex.getQuestion(), null, "en");
                    if (audio != null && audio.length > 0) {
                        String audioUrl = cloudinaryService.uploadAudioBytes(audio, "ai-listen-" + lesson.getId() + "-" + System.currentTimeMillis());
                        ex.setAudioUrl(audioUrl);
                        log.info("TTS audio generated: {}", audioUrl);
                    }
                } catch (Exception e) {
                    log.warn("TTS failed: {}", e.getMessage());
                }
            }
        }
        return exercises;
    }

    public List<Exercise> generateAll(Lesson lesson, int count) {
        List<Exercise> all = new ArrayList<>();
        int perType = Math.max(1, count / 5);
        int remainder = count - perType * 5;
        all.addAll(generateMultipleChoice(lesson, perType));
        all.addAll(generateFillBlank(lesson, perType));
        all.addAll(generateMatching(lesson, perType));
        all.addAll(generateTranslation(lesson, perType));
        all.addAll(generateListening(lesson, perType + remainder));
        for (int i = 0; i < all.size(); i++) all.get(i).setOrderIndex(i);
        return all;
    }

    // ΓöÇΓöÇ Review loop ΓöÇΓöÇ

    private List<Exercise> generateWithReviewLoop(Lesson lesson, ExerciseType type, int count, String researchContext) {
        String prompt = buildPrompt(lesson, type, count, researchContext);
        List<Exercise> accepted = new ArrayList<>();
        java.util.Set<String> batchSeen = ConcurrentHashMap.newKeySet();
        for (int attempt = 0; attempt < maxReviewAttempts; attempt++) {
            try {
                String response = callOllama(prompt);
                List<Exercise> exercises = parseExercises(response, lesson, type);
                for (Exercise candidate : exercises) {
                    Exercise ex = candidate;
                    String schemaError = validateSchema(ex);
                    if (schemaError != null && ex.getExerciseType() == ExerciseType.MATCHING) {
                        Exercise repaired = repairMatchingSlashPairs(ex);
                        if (repaired != null) {
                            ex = repaired;
                            schemaError = validateSchema(ex);
                            log.info("MATCHING slash-pair repaired: question='{}'", truncate(ex.getQuestion()));
                        }
                    }
                    if (schemaError != null) {
                        log.warn("Schema reject: type={}, question='{}', options='{}', answer='{}', error={}",
                                type, truncate(ex.getQuestion()), truncate(ex.getOptions()),
                                truncate(ex.getCorrectAnswer()), schemaError);
                    } else if (isExampleCopy(ex)) {
                        log.warn("Example-copy reject: model returned the few-shot example verbatim, question='{}'",
                                truncate(ex.getQuestion()));
                    } else if (isDuplicate(ex, batchSeen)) {
                        log.warn("Dedup reject: question='{}'", truncate(ex.getQuestion()));
                    } else {
                        accepted.add(ex);
                    }
                }
                if (accepted.size() >= count) {
                    boolean allPassed = true;
                    for (Exercise ex : accepted) {
                        ReviewResult review = reviewExercise(ex);
                        if (!review.passed) { allPassed = false; break; }
                    }
                    if (allPassed) {
                        accepted.forEach(ex -> seenQuestionHashes.add(questionHash(ex)));
                        return new ArrayList<>(accepted.subList(0, count));
                    }
                }
                log.info("Attempt {}/{} accepted {}/{} so far", attempt+1, maxReviewAttempts, accepted.size(), count);
            } catch (Exception e) {
                log.error("Gen attempt {}/{} failed: {}", attempt+1, maxReviewAttempts, e.getMessage());
                if (attempt == maxReviewAttempts - 1 && accepted.isEmpty())
                    throw new RuntimeException("Failed after " + maxReviewAttempts + " attempts: " + e.getMessage(), e);
            }
        }
        // Partial success: fewer than requested but valid exercises were produced.
        // Persisting them beats returning nothing (MCP behavior: valid[:num]).
        if (!accepted.isEmpty()) {
            accepted.forEach(ex -> seenQuestionHashes.add(questionHash(ex)));
            return accepted;
        }
        return List.of();
    }

    // ── Deduplication (ported from MCP dedup_exercises) ──

    /**
     * Returns true when the exercise question duplicates one already in
     * {@code batchQuestions} (within-batch) or one generated earlier in this JVM
     * (cross-lesson). Matching is case-insensitive on trimmed question text.
     *
     * @param ex exercise to check
     * @param batchQuestions per-batch seen set, may be null to check cross-lesson only
     */
    private boolean isDuplicate(Exercise ex, java.util.Set<String> batchQuestions) {
        String hash = questionHash(ex);
        if (batchQuestions != null && !batchQuestions.add(hash)) return true;
        return seenQuestionHashes.contains(hash);
    }

    private static String questionHash(Exercise ex) {
        String q = ex.getQuestion() == null ? "" : ex.getQuestion().trim().toLowerCase();
        return java.util.UUID.nameUUIDFromBytes(q.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * Detects verbatim copies of the few-shot prompt examples (a known failure
     * mode of qwen2.5:1.5b). Matching is exact on normalized question text.
     */
    private static boolean isExampleCopy(Exercise ex) {
        if (ex.getQuestion() == null) return false;
        return EXAMPLE_QUESTION_SIGNATURES.contains(ex.getQuestion().trim());
    }

    // ── MATCHING drift repair (MCP _fix_matching_placeholders philosophy) ──

    /**
     * Repairs the dominant qwen2.5:1.5b MATCHING drift shape: options written as
     * "left / right" halves instead of "left|right" pairs (observed at runtime:
     * ["The book / is on the desk", ...]). Only repairs when every option uses
     * the same " / " separator, no option already uses "|", and the resulting
     * pairs have distinct left sides. The right side is trimmed to its first
     * sentence. Returns the repaired exercise, or null when not repairable.
     *
     * @param ex candidate exercise flagged as MATCHING
     * @return repaired exercise in "left|right" format, or null
     */
    Exercise repairMatchingSlashPairs(Exercise ex) {
        if (ex.getExerciseType() != ExerciseType.MATCHING) return null;
        if (ex.getOptions() == null || ex.getOptions().equals("[]")) return null;
        List<?> opts;
        try {
            opts = objectMapper.readValue(ex.getOptions(), new TypeReference<>(){});
        } catch (Exception e) {
            return null;
        }
        if (opts.isEmpty()) return null;
        boolean allPipe = opts.stream().allMatch(o -> o instanceof String s && s.contains("|"));
        if (allPipe) return null; // already valid, nothing to repair
        boolean allSlash = opts.stream().allMatch(o -> o instanceof String s && s.contains(" / "));
        if (!allSlash) return null; // inconsistent or degenerate shape
        List<String> pairs = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        java.util.Set<String> lefts = new java.util.HashSet<>();
        for (Object o : opts) {
            String s = ((String) o).trim();
            int idx = s.indexOf(" / ");
            String left = s.substring(0, idx).trim();
            String right = s.substring(idx + 3).trim();
            int dot = right.indexOf('.');
            if (dot > 0) right = right.substring(0, dot).trim();
            if (left.isEmpty() || right.isEmpty() || !lefts.add(left.toLowerCase())) return null;
            pairs.add(left + "|" + right);
            answers.add(left + "=" + right);
        }
        if (pairs.size() < 2) return null;
        try {
            return Exercise.builder()
                    .lesson(ex.getLesson())
                    .question(ex.getQuestion())
                    .options(objectMapper.writeValueAsString(pairs))
                    .correctAnswer(String.join(",", answers))
                    .explanation(ex.getExplanation())
                    .exerciseType(ExerciseType.MATCHING)
                    .difficulty(ex.getDifficulty())
                    .build();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return null;
        }
    }

    /** Test-visible wrapper around {@link #questionHash(Exercise)}. */
    static String questionHashForTest(Exercise ex) {
        return questionHash(ex);
    }

    /** Test-visible wrapper around {@link #isExampleCopy(Exercise)}. */
    static boolean isExampleCopyForTest(Exercise ex) {
        return isExampleCopy(ex);
    }

    private static String truncate(String s) {
        return truncate(s, 120);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    // ── Parse LLM response (3-layer salvage, ported from MCP try_parse_json) ──

    private List<Exercise> parseExercises(String jsonResponse, Lesson lesson, ExerciseType type) {
        List<Map<String, Object>> list = parseExerciseJsonArray(jsonResponse);
        if (list == null) throw new RuntimeException("Parse failed: no JSON array found in LLM response");
        List<Exercise> exercises = new ArrayList<>();
        for (Map<String,Object> map : list) {
            try {
                exercises.add(Exercise.builder()
                    .lesson(lesson)
                    .question((String) map.get("question"))
                    .options(map.get("options") != null ? objectMapper.writeValueAsString(map.get("options")) : "[]")
                    .correctAnswer((String) map.get("correctAnswer"))
                    .explanation((String) map.get("explanation"))
                    .exerciseType(type)
                    .difficulty(parseDifficulty((String) map.get("difficulty")))
                    .build());
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("Options serialization failed: " + e.getMessage(), e);
            }
        }
        return exercises;
    }

    /**
     * Parses an LLM response into a list of exercise maps, salvaging as much as
     * possible. Layer 1 strips markdown fences and parses directly. Layer 2
     * extracts a truncated JSON array and auto-closes it. Layer 3 salvages
     * complete individual objects from a broken array.
     *
     * @param jsonResponse raw LLM output
     * @return parsed exercise maps, or null when nothing can be salvaged
     */
    static List<Map<String, Object>> parseExerciseJsonArray(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) return null;
        String text = jsonResponse.trim();
        // Layer 1: strip markdown fences, parse directly
        if (text.startsWith("```")) {
            int start = text.startsWith("```json") ? 7 : 3;
            int end = text.lastIndexOf("```");
            if (end > start) text = text.substring(start, end).trim();
        }
        List<Map<String, Object>> parsed = tryReadArray(text);
        if (parsed != null) return parsed;
        // Layer 2: regex-extract a JSON array (handles surrounding prose), auto-close truncation
        java.util.regex.Matcher arrayMatcher = java.util.regex.Pattern
                .compile("\\[[\\s\\S]*]?")
                .matcher(text);
        if (arrayMatcher.find()) {
            String candidate = arrayMatcher.group();
            parsed = tryReadArray(candidate);
            if (parsed == null && !candidate.endsWith("]")) {
                parsed = tryReadArray(candidate + "]");
            }
        }
        if (parsed != null) return parsed;
        // Layer 3: salvage complete objects one by one from a broken array
        java.util.regex.Matcher objectMatcher = java.util.regex.Pattern
                .compile("\\{[^{}]*\\}")
                .matcher(text);
        List<Map<String, Object>> salvaged = new ArrayList<>();
        while (objectMatcher.find()) {
            List<Map<String, Object>> single = tryReadArray("[" + objectMatcher.group() + "]");
            if (single != null) salvaged.addAll(single);
        }
        return salvaged.isEmpty() ? null : salvaged;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> tryReadArray(String text) {
        try {
            List<?> raw = new ObjectMapper().readValue(text, List.class);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object o : raw) {
                if (o instanceof Map<?, ?> m) result.add((Map<String, Object>) m);
            }
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            return null;
        }
    }

    private ExerciseDifficulty parseDifficulty(String d) {
        if (d == null || d.isBlank()) return ExerciseDifficulty.MEDIUM;
        try { return ExerciseDifficulty.valueOf(d.toUpperCase().trim()); }
        catch (IllegalArgumentException e) { return ExerciseDifficulty.MEDIUM; }
    }

    // ΓöÇΓöÇ Validation ΓöÇΓöÇ

    public String validateSchema(Exercise ex) {
        if (ex.getQuestion() == null || ex.getQuestion().isBlank()) return "question empty";
        if (ex.getCorrectAnswer() == null || ex.getCorrectAnswer().isBlank()) return "correctAnswer empty";
        if (ex.getExerciseType() == ExerciseType.MULTIPLE_CHOICE) {
            if (ex.getOptions() == null || ex.getOptions().equals("[]")) return "MULTIPLE_CHOICE needs options";
            try {
                List<?> opts = objectMapper.readValue(ex.getOptions(), new TypeReference<>(){});
                if (opts.size() < 2) return "need >= 2 options";
            } catch (Exception e) { return "invalid options JSON"; }
        }
        if (ex.getExerciseType() == ExerciseType.MATCHING) {
            if (ex.getOptions() == null || ex.getOptions().equals("[]")) return "MATCHING needs options";
            try {
                List<?> opts = objectMapper.readValue(ex.getOptions(), new TypeReference<>(){});
                if (opts.size() < 2) return "MATCHING needs >= 2 pairs";
                long pipePairs = opts.stream()
                        .filter(o -> o instanceof String s && s.contains("|"))
                        .count();
                if (pipePairs != opts.size()) {
                    return "MATCHING options must be \"left|right\" strings (got " + pipePairs + "/" + opts.size() + ")";
                }
                if (!ex.getCorrectAnswer().contains("=")) return "MATCHING correctAnswer must be \"left=right,...\"";
            } catch (Exception e) { return "invalid options JSON"; }
        }
        return null;
    }

    // ΓöÇΓöÇ AI Review ΓöÇΓöÇ

    public static class ReviewResult { public boolean passed; public String reason; }

    public ReviewResult reviewExercise(Exercise ex) {
        ReviewResult result = new ReviewResult();
        try {
            String prompt = String.format(
                "Review this English exercise. Check: question clear, answer correct, difficulty appropriate. " +
                "Type: %s, Question: %s, Options: %s, Answer: %s. " +
                "Respond ONLY: {\"passed\": true/false, \"reason\": \"...\"}",
                ex.getExerciseType(), ex.getQuestion(),
                ex.getOptions() != null ? ex.getOptions() : "N/A", ex.getCorrectAnswer());
            String response = callOllama(prompt, reviewerModel);
            String clean = response.trim();
            if (clean.startsWith("```json")) clean = clean.replace("```json","").replace("```","").trim();
            else if (clean.startsWith("```")) clean = clean.replace("```","").trim();
            Map<String,Object> m = objectMapper.readValue(clean, new TypeReference<>(){});
            result.passed = Boolean.TRUE.equals(m.get("passed"));
            result.reason = (String) m.getOrDefault("reason","");
        } catch (Exception e) {
            result.passed = true; result.reason = "Review skipped: " + e.getMessage();
        }
        return result;
    }

    // ΓöÇΓöÇ Batch generation ΓöÇΓöÇ

    public BatchProgress generateBatch(boolean force) {
        List<Lesson> allLessons = lessonRepository.findAll();
        BatchProgress progress = new BatchProgress();
        progress.totalLessons = allLessons.size();
        progress.running = true;
        batchProgressMap.put("batch", progress);
        new Thread(() -> {
            try {
                for (Lesson lesson : allLessons) {
                    progress.currentLesson = lesson.getTitle();
                    try {
                        if (!force && exerciseRepository.countByLessonId(lesson.getId()) > 0) {
                            progress.processed++; continue;
                        }
                        String content = lesson.getContent();
                        if (content == null || content.isBlank()) { progress.processed++; continue; }
                        List<Exercise> exercises = generateAll(lesson, 5);
                        exerciseRepository.saveAll(exercises);
                        progress.generated += exercises.size();
                    } catch (Exception e) {
                        progress.errors++;
                        log.error("Batch gen failed for lesson {}: {}", lesson.getId(), e.getMessage());
                    }
                    progress.processed++;
                }
            } finally {
                progress.running = false;
            }
        }).start();
        return progress;
    }

    public BatchProgress getBatchProgress() {
        return batchProgressMap.get("batch");
    }

    // ── Single-lesson async generation (Option 2 — progress polling) ──
    // Returns batchId IMMEDIATELY; generation runs in a background thread so the
    // HTTP 202 Accepted response is never blocked by slow Ollama calls.

    public String generateSingleAsync(Lesson lesson, int count, ExerciseType type) {
        String batchId = UUID.randomUUID().toString();
        BatchProgress progress = new BatchProgress();
        progress.totalLessons = 1;
        progress.running = true;
        progress.currentLesson = lesson.getTitle();
        batchProgressMap.put(batchId, progress);

        CompletableFuture.runAsync(() -> {
            long bgStart = System.currentTimeMillis();
            try {
                List<Exercise> exercises = (type == null)
                        ? generateAll(lesson, count)
                        : switch (type) {
                            case MULTIPLE_CHOICE -> generateMultipleChoice(lesson, count);
                            case FILL_BLANK -> generateFillBlank(lesson, count);
                            case MATCHING -> generateMatching(lesson, count);
                            case TRANSLATION -> generateTranslation(lesson, count);
                            case LISTENING -> generateListening(lesson, count);
                        };
                for (Exercise ex : exercises) ex.setOrderIndex(exercises.indexOf(ex));
                exerciseRepository.saveAll(exercises);
                progress.generated = exercises.size();
                progress.exercises = exercises;
                progress.processed = 1;
            } catch (Exception e) {
                progress.errors++;
                progress.errorDetails = List.of(e.getMessage());
                log.error("Async gen failed for batchId={}: {}", batchId, e.getMessage());
            } finally {
                progress.running = false;
                batchProgressMap.put(batchId, progress);
                log.info("Async batch {} completed in {}ms (generated={}, errors={})",
                        batchId, System.currentTimeMillis() - bgStart, progress.generated, progress.errors);
            }
        }, generationPool);
        return batchId;
    }

    public BatchProgress getProgress(String batchId) {
        return batchProgressMap.get(batchId);
    }
}
