package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.DashboardStatsDTO;

public interface DashboardService {
    DashboardStatsDTO getDashboardStats(String email);
}
