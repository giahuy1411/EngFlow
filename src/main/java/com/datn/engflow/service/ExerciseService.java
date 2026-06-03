package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.ExerciseSubmitRequest;
import com.datn.engflow.model.dto.response.ExerciseSubmissionDTO;

import java.util.List;

public interface ExerciseService {
    boolean submitAnswer(String userEmail, ExerciseSubmitRequest request);
    List<ExerciseSubmissionDTO> getExerciseSubmissions(String userEmail);
}
