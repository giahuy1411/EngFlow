package com.datn.engflow.model.dto.projection;

import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;

/**
 * Flat projection for the lesson exercise list (audit-v9 F108).
 *
 * <p>Why it exists: the list used to hydrate {@code Exercise} entities through
 * {@code JOIN FETCH e.lesson}. Every row therefore carried the lesson's
 * {@code content} and {@code content_original} NVARCHAR(MAX) columns. Measured
 * on 2026-09-17 against lesson 651 (published, 99 exercises, content 26 kB +
 * content_original 77 kB): ~5,724 logical reads and ~14 ms of server time per
 * call at 153 ms median end-to-end, versus ~120 reads for a small lesson; the
 * single worst planned execution in the cache was 197,337 reads / 3,250 ms.
 *
 * <p>This projection selects only the columns {@code ExerciseResponse} renders,
 * the same pattern already used by {@code LessonListProjection} /
 * {@code LessonTitle}.
 */
public interface ExerciseLessonProjection {

    Long getId();

    Long getLessonId();

    String getLessonTitle();

    String getQuestion();

    String getOptions();

    String getCorrectAnswer();

    ExerciseType getExerciseType();

    ExerciseDifficulty getDifficulty();

    String getExplanation();

    String getImageUrl();

    String getAudioUrl();

    Integer getOrderIndex();
}
