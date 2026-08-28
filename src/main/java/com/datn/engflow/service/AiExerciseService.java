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
                    .timeout(Duration.ofSeconds(30))
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

    private String buildPrompt(Lesson lesson, ExerciseType type, int count, String researchContext) {
        String lessonContent = buildBasePrompt(lesson);
        String typeInstruction = switch (type) {
            case MULTIPLE_CHOICE -> "multiple choice questions with 4 options each";
            case FILL_BLANK -> "fill-in-the-blank questions where the student fills the missing word";
            case MATCHING -> "matching questions (options as JSON array of \"item:::match\" strings, correctAnswer is \"item:::match\")";
            case TRANSLATION -> "translation exercises from English to Vietnamese";
            case LISTENING -> "listening exercises (question is text to be spoken, correctAnswer is expected transcription)";
        };
        String researchSection = researchContext != null && !researchContext.isBlank()
                ? "\n\nAdditional research:\n" + researchContext : "";
        return String.format(
            "You are an English exercise generator. Create %d %s. Return ONLY a valid JSON array (no markdown). " +
            "Each object: {\"question\": string, \"options\": array, \"correctAnswer\": string, \"explanation\": string, \"difficulty\": \"EASY|MEDIUM|HARD\"}. " +
            "Lesson: %s. Content: %s%s. Generate exactly %d exercises.",
            count, typeInstruction, lesson.getTitle(), lessonContent, researchSection, count);
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
        for (int attempt = 0; attempt < maxReviewAttempts; attempt++) {
            try {
                String response = callOllama(prompt);
                List<Exercise> exercises = parseExercises(response, lesson, type);
                List<Exercise> valid = new ArrayList<>();
                for (Exercise ex : exercises) {
                    if (validateSchema(ex) == null) valid.add(ex);
                }
                if (valid.size() >= count) {
                    boolean allPassed = true;
                    for (Exercise ex : valid) {
                        ReviewResult review = reviewExercise(ex);
                        if (!review.passed) { allPassed = false; break; }
                    }
                    if (allPassed) return valid.subList(0, count);
                }
                if (valid.size() > 0 && attempt == maxReviewAttempts - 1) return valid;
                log.info("Attempt {}/{} generated {} valid, retrying", attempt+1, maxReviewAttempts, valid.size());
            } catch (Exception e) {
                log.error("Gen attempt {}/{} failed: {}", attempt+1, maxReviewAttempts, e.getMessage());
                if (attempt == maxReviewAttempts - 1) throw new RuntimeException("Failed after " + maxReviewAttempts + " attempts: " + e.getMessage(), e);
            }
        }
        return List.of();
    }

    // ΓöÇΓöÇ Parse LLM response ΓöÇΓöÇ

    private List<Exercise> parseExercises(String jsonResponse, Lesson lesson, ExerciseType type) {
        try {
            String clean = jsonResponse.trim();
            if (clean.startsWith("```json")) clean = clean.replace("```json","").replace("```","").trim();
            else if (clean.startsWith("```")) clean = clean.replace("```","").trim();
            List<Map<String,Object>> list = objectMapper.readValue(clean, new TypeReference<>(){});
            List<Exercise> exercises = new ArrayList<>();
            for (Map<String,Object> map : list) {
                exercises.add(Exercise.builder()
                    .lesson(lesson)
                    .question((String) map.get("question"))
                    .options(map.get("options") != null ? objectMapper.writeValueAsString(map.get("options")) : "[]")
                    .correctAnswer((String) map.get("correctAnswer"))
                    .explanation((String) map.get("explanation"))
                    .exerciseType(type)
                    .difficulty(parseDifficulty((String) map.get("difficulty")))
                    .build());
            }
            return exercises;
        } catch (Exception e) {
            throw new RuntimeException("Parse failed: " + e.getMessage(), e);
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
