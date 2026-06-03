package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.ProgressResponse;

public interface ProgressService {
    ProgressResponse getProgressSummary(String userEmail);
}
