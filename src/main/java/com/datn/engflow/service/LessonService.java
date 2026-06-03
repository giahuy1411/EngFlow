package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.LessonRequest;
import com.datn.engflow.model.dto.response.LessonResponse;
import com.datn.engflow.model.entity.Lesson;

import java.util.List;

public interface LessonService {
    List<LessonResponse> getAllLessons(String userEmail);
    LessonResponse getLessonDetails(Long lessonId, String userEmail);
    LessonResponse createLesson(LessonRequest lessonRequest);
    LessonResponse updateLesson(Long id, LessonRequest lessonRequest);
    void deleteLesson(Long id);
}
