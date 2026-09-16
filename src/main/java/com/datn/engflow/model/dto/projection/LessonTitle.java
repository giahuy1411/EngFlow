package com.datn.engflow.model.dto.projection;

/**
 * Lesson id + title only. Used by the admin exercise page so the list can label each
 * row without loading {@code content} / {@code content_original} (measured: fetching the
 * joined lesson cost 320 LOB logical reads and 32ms per 20-row page, versus 0 and 1ms
 * when only the title is selected).
 */
public interface LessonTitle {

    Long getLessonId();

    String getTitle();
}
