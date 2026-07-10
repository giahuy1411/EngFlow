package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.ExerciseRequest;
import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.dto.response.ExerciseGradeItem;
import com.datn.engflow.model.dto.response.ExerciseResponse;
import com.datn.engflow.model.dto.response.GradeResponse;
import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final LessonRepository lessonRepository;

    // --- CRUD ---

    public List<ExerciseResponse> getExercisesByLesson(Long lessonId) {
        List<Exercise> exercises = exerciseRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
        if (!exercises.isEmpty()) {
            return exercises.stream().map(this::toResponse).toList();
        }
        // Fallback: parent lesson has no exercises — find sub-lesson by title
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson != null && lesson.getTitle() != null) {
            // Extract base topic: remove prefix and suffix
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
                        return candidateExercises.stream().map(this::toResponse).toList();
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
        List<Exercise> exercises = exerciseRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
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

    // --- Admin query ---

    public List<ExerciseResponse> getAllExercises(Long lessonId, String type, String difficulty, String search) {
        if (lessonId != null) {
            return exerciseRepository.findByLessonIdOrderByOrderIndexAsc(lessonId)
                    .stream().map(this::toResponse).toList();
        }
        return exerciseRepository.findAll().stream().map(this::toResponse).toList();
    }

    // --- Mapper ---

    private ExerciseResponse toResponse(Exercise ex) {
        return ExerciseResponse.builder()
                .id(ex.getId())
                .lessonId(ex.getLesson().getId())
                .question(ex.getQuestion())
                .options(ex.getOptions())
                .correctAnswer(ex.getCorrectAnswer())
                .exerciseType(ex.getExerciseType().name())
                .difficulty(ex.getDifficulty() != null ? ex.getDifficulty().name() : null)
                .explanation(ex.getExplanation())
                .imageUrl(ex.getImageUrl())
                .audioUrl(ex.getAudioUrl())
                .orderIndex(ex.getOrderIndex())
                .build();
    }
}
