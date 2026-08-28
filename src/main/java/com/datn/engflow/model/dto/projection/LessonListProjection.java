package com.datn.engflow.model.dto.projection;

import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.model.enums.SkillType;

/**
 * Lightweight projection for the lesson list page.
 * <p>
 * Selecting only the columns the list UI actually renders avoids hydrating the
 * large NVARCHAR(MAX) {@code content}/{@code contentOriginal} columns, which
 * dominated page reads (the lessons table is ~161 MB for only 1,472 rows).
 * </p>
 */
public interface LessonListProjection {

    Long getId();

    String getTitle();

    String getDescription();

    LessonLevel getLevel();

    String getCategory();

    Integer getDurationMinutes();

    String getThumbnailUrl();

    String getAudioUrl();

    SkillType getSkillType();

    Integer getOrderIndex();
}
