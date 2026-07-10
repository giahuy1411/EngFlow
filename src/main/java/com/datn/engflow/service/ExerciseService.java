package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.ExerciseRequest;
import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.dto.response.*;
import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.ExerciseAttempt;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.repository.ExerciseAttemptRepository;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final LessonRepository lessonRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final UserRepository userRepository;

    // --- CRUD ---

    public List<ExerciseResponse> getExercisesByLesson(Long lessonId, boolean includeAnswers) {
        List<Exercise> exercises = findExercisesForLesson(lessonId);
        return exercises.stream()
                .map(ex -> toResponse(ex, includeAnswers))
                .toList();
    }

    private List<Exercise> findExercisesForLesson(Long lessonId) {
        List<Exercise> exercises = exerciseRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
        if (!exercises.isEmpty()) {
            return exercises;
        }
        // Fallback: parent lesson has no exercises — find sub-lesson by title
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson != null && lesson.getTitle() != null) {
            String base = lesson.getTitle()
                    .replaceAll("^English (Grammar|Vocabulary|Reading|Listening|Speaking|Writing) Exercises for [A-Z][12] – ", "")
                    .replaceAll(" - (GRAMMAR|VOCABULARY|LISTENING|READING|SPEAKING|WRITING|WORD_SKILLS)$", "")
                    .trim();
            if (!base.isEmpty()) {
                List<Lesson> candidates = lessonRepository.findByTitleContainingIgnoreCase(base);
                for (Lesson candidate : candidates) {
                    if (candidate.getId().equals(lessonId)) continue;
                    List<Exercise> candidateExercises = exerciseRepository.findByLessonIdOrderByOrderIndexAsc(candidate.getId());
                    if (!candidateExercises.isEmpty()) {
                        return candidateExercises;
                    }
                }
            }
        }
        return List.of();
    }

    public ExerciseResponse getExercise(Long id) {
        Exercise ex = exerciseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Exercise not found: " + id));
        return toResponse(ex);
    }

    @Transactional
    public ExerciseResponse createExercise(ExerciseRequest request) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found: " + request.getLessonId()));

        Exercise exercise = Exercise.builder()
                .lesson(lesson)
                .question(request.getQuestion())
                .options(request.getOptions())
                .correctAnswer(request.getCorrectAnswer())
                .exerciseType(ExerciseType.valueOf(request.getExerciseType()))
                .difficulty(request.getDifficulty() != null
                        ? ExerciseDifficulty.valueOf(request.getDifficulty()) : null)
                .explanation(request.getExplanation())
                .imageUrl(request.getImageUrl())
                .audioUrl(request.getAudioUrl())
                .orderIndex(request.getOrderIndex())
                .build();

        exercise = exerciseRepository.save(exercise);
        return toResponse(exercise);
    }

    @Transactional
    public ExerciseResponse updateExercise(Long id, ExerciseRequest request) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Exercise not found: " + id));

        if (request.getQuestion() != null) exercise.setQuestion(request.getQuestion());
        if (request.getOptions() != null) exercise.setOptions(request.getOptions());
        if (request.getCorrectAnswer() != null) exercise.setCorrectAnswer(request.getCorrectAnswer());
        if (request.getExerciseType() != null) exercise.setExerciseType(ExerciseType.valueOf(request.getExerciseType()));
        if (request.getDifficulty() != null) exercise.setDifficulty(ExerciseDifficulty.valueOf(request.getDifficulty()));
        if (request.getExplanation() != null) exercise.setExplanation(request.getExplanation());
        if (request.getImageUrl() != null) exercise.setImageUrl(request.getImageUrl());
        if (request.getAudioUrl() != null) exercise.setAudioUrl(request.getAudioUrl());
        if (request.getOrderIndex() != null) exercise.setOrderIndex(request.getOrderIndex());

        exercise = exerciseRepository.save(exercise);
        return toResponse(exercise);
    }

    @Transactional
    public void deleteExercise(Long id) {
        if (!exerciseRepository.existsById(id)) {
            throw new EntityNotFoundException("Exercise not found: " + id);
        }
        exerciseRepository.deleteById(id);
    }

    // --- Grading ---

    @Transactional
    public GradeResponse gradeExercises(Long lessonId, GradeRequest request) {
        List<Exercise> exercises = findExercisesForLesson(lessonId);
        List<ExerciseGradeItem> results = new ArrayList<>();
        int score = 0;

        if (request.getAnswers() == null) {
            return GradeResponse.builder()
                    .results(results)
                    .score(0)
                    .total(0)
                    .percentage(0)
                    .build();
        }

        for (GradeRequest.AnswerItem item : request.getAnswers()) {
            if (item.getExerciseId() == null) continue;

            Exercise ex = exercises.stream()
                    .filter(e -> e.getId().equals(item.getExerciseId()))
                    .findFirst().orElse(null);

            if (ex == null) continue;

            boolean correct = normalizeAnswer(item.getUserAnswer())
                    .equals(normalizeAnswer(ex.getCorrectAnswer()));
            if (correct) score++;

            results.add(ExerciseGradeItem.builder()
                    .exerciseId(ex.getId())
                    .correct(correct)
                    .userAnswer(item.getUserAnswer())
                    .correctAnswer(ex.getCorrectAnswer())
                    .build());
        }

        double pct = request.getAnswers().isEmpty() ? 0 :
                (double) score / request.getAnswers().size() * 100;

        return GradeResponse.builder()
                .results(results)
                .score(score)
                .total(request.getAnswers().size())
                .percentage(Math.round(pct * 100.0) / 100.0)
                .build();
    }

    private String normalizeAnswer(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public List<ExerciseResponse> getAllExercises(Long lessonId, String type, String difficulty, String search) {
        if (lessonId != null) {
            return exerciseRepository.findByLessonIdOrderByOrderIndexAsc(lessonId)
                    .stream().map(e -> toResponse(e, true)).toList();
        }
        return exerciseRepository.findAll().stream().map(e -> toResponse(e, true)).toList();
    }

    // --- Exercise Attempts ---

    @Transactional
    public GradeResponse submitExercises(Long lessonId, GradeRequest request, String userEmail) {
        GradeResponse grade = gradeExercises(lessonId, request);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userEmail));

        // Build details JSON
        String detailsJson = grade.getResults().stream()
                .map(item -> {
                    Exercise ex = exerciseRepository.findById(item.getExerciseId()).orElse(null);
                    return "{\"exerciseId\":" + item.getExerciseId()
                            + ",\"question\":" + (ex != null ? escapeJson(ex.getQuestion()) : "null")
                            + ",\"userAnswer\":" + escapeJson(item.getUserAnswer())
                            + ",\"correctAnswer\":" + escapeJson(item.getCorrectAnswer())
                            + ",\"isCorrect\":" + item.isCorrect()
                            + ",\"explanation\":" + (ex != null ? escapeJson(ex.getExplanation()) : "null")
                            + "}";
                })
                .collect(Collectors.joining(",", "[", "]"));

        BigDecimal pct = BigDecimal.valueOf(grade.getPercentage())
                .setScale(2, RoundingMode.HALF_UP);

        ExerciseAttempt attempt = ExerciseAttempt.builder()
                .user(user)
                .lessonId(lessonId)
                .score(grade.getScore())
                .total(grade.getTotal())
                .percentage(pct)
                .details(detailsJson)
                .completedAt(LocalDateTime.now())
                .build();

        attemptRepository.save(attempt);

        return grade;
    }

    private String escapeJson(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    public List<AttemptHistoryResponse> getAttemptHistory(Long lessonId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userEmail));
        return attemptRepository.findByUserIdAndLessonIdOrderByCompletedAtDesc(user.getId(), lessonId)
                .stream()
                .map(a -> AttemptHistoryResponse.builder()
                        .id(a.getId())
                        .score(a.getScore())
                        .total(a.getTotal())
                        .percentage(a.getPercentage())
                        .completedAt(a.getCompletedAt())
                        .build())
                .toList();
    }

    public AttemptDetailResponse getAttemptDetail(Long lessonId, Long attemptId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userEmail));
        ExerciseAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found: " + attemptId));

        // Parse details JSON (simple parse — trust format)
        String raw = attempt.getDetails();
        List<AttemptDetailResponse.AttemptDetailItem> items = new ArrayList<>();
        if (raw != null && raw.startsWith("[")) {
            String itemsStr = raw.substring(1, raw.length() - 1);
            if (!itemsStr.isBlank()) {
                for (String part : splitJsonArray(itemsStr)) {
                    try {
                        Long eId = extractLong(part, "exerciseId");
                        String q = extractString(part, "question");
                        String ua = extractString(part, "userAnswer");
                        String ca = extractString(part, "correctAnswer");
                        boolean ic = extractBoolean(part, "isCorrect");
                        String exp = extractString(part, "explanation");
                        items.add(AttemptDetailResponse.AttemptDetailItem.builder()
                                .exerciseId(eId).question(q).userAnswer(ua)
                                .correctAnswer(ca).isCorrect(ic).explanation(exp)
                                .build());
                    } catch (Exception ignored) {}
                }
            }
        }

        return AttemptDetailResponse.builder()
                .id(attempt.getId())
                .score(attempt.getScore())
                .total(attempt.getTotal())
                .percentage(attempt.getPercentage())
                .completedAt(attempt.getCompletedAt())
                .details(items)
                .build();
    }

    private List<String> splitJsonArray(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') { depth++; if (depth == 1) start = i; }
            else if (c == '}') { depth--; if (depth == 0) parts.add(s.substring(start, i + 1)); }
        }
        return parts;
    }

    private Long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int valStart = idx + search.length();
        int valEnd = valStart;
        while (valEnd < json.length() && Character.isDigit(json.charAt(valEnd))) valEnd++;
        try { return Long.parseLong(json.substring(valStart, valEnd)); } catch (NumberFormatException e) { return null; }
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int valStart = json.indexOf('"', idx + search.length()) + 1;
        if (valStart == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = valStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"') { sb.append('"'); i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else if (next == 'n') { sb.append('\n'); i++; }
                else { sb.append(c); }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean extractBoolean(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return false;
        return json.substring(idx + search.length()).startsWith("true");
    }

    // --- Mappers ---

    private ExerciseResponse toResponse(Exercise ex) {
        return toResponse(ex, false);
    }

    private ExerciseResponse toResponse(Exercise ex, boolean includeAnswers) {
        return ExerciseResponse.builder()
                .id(ex.getId())
                .lessonId(ex.getLesson().getId())
                .question(ex.getQuestion())
                .options(ex.getOptions())
                .correctAnswer(includeAnswers ? ex.getCorrectAnswer() : null)
                .exerciseType(ex.getExerciseType().name())
                .difficulty(ex.getDifficulty() != null ? ex.getDifficulty().name() : null)
                .explanation(includeAnswers ? ex.getExplanation() : null)
                .imageUrl(ex.getImageUrl())
                .audioUrl(ex.getAudioUrl())
                .orderIndex(ex.getOrderIndex())
                .build();
    }
}
