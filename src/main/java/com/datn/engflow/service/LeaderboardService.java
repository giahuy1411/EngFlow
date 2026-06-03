package com.datn.engflow.service;

import com.datn.engflow.model.dto.response.LeaderboardEntryDTO;

import java.util.List;

public interface LeaderboardService {
    List<LeaderboardEntryDTO> getLeaderboard(int limit);
}
