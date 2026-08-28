package com.datn.engflow.service;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * class ExerciseSeedService.
 */
public class ExerciseSeedService {

    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final HtmlParserService htmlParserService;

    /**
     * Parse ALL lessons' HTML content and generate Exercise entities.
     * Skips lessons that already have exercises.
     */
    @Transactional
    public SeedResult seedAllLessons() {
        return seedAllLessons(false);
    }

    /**
     * Parse ALL lessons' HTML content and generate Exercise entities.
     * @param force if true, deletes all existing exercise data first
     */
    @Transactional
    public SeedResult seedAllLessons(boolean force) {
        if (force) {
            log.warn("Force mode: deleting all existing exercises");
            exerciseRepository.deleteAllInBatch();
        }

        List<Lesson> allLessons = lessonRepository.findAll();
        int totalParsed = 0;
        int totalSkipped = 0;
        int totalErrors = 0;
        List<String> errors = new ArrayList<>();

        for (Lesson lesson : allLessons) {
            try {
                if (!force) {
                    long existing = exerciseRepository.countByLessonId(lesson.getId());
                    if (existing > 0) {
                        totalSkipped++;
                        continue;
                    }
                }

                String content = lesson.getContent();
                if (content == null || content.isBlank()) {
                    totalSkipped++;
                    continue;
                }

                List<Exercise> exercises = htmlParserService.parseExercises(lesson, content);
                if (!exercises.isEmpty()) {
                    exerciseRepository.saveAll(exercises);
                    totalParsed += exercises.size();
                    log.info("Lesson {}: generated {} exercises", lesson.getId(), exercises.size());
                } else {
                    totalSkipped++;
                }
            } catch (Exception e) {
                totalErrors++;
                String msg = "Lesson " + lesson.getId() + ": " + e.getMessage();
                errors.add(msg);
                log.error("Error seeding lesson {}: {}", lesson.getId(), e.getMessage());
            }
        }

        return new SeedResult(totalParsed, totalSkipped, totalErrors, errors);
    }

/**
 * record SeedResult.
 */
    public record SeedResult(int totalParsed, int totalSkipped, int totalErrors, List<String> errors) {}
}
