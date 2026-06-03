package com.datn.engflow.controller;

import com.datn.engflow.model.dto.response.LeaderboardEntryDTO;
import com.datn.engflow.service.LeaderboardService;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboard(
            @Max(value = 100, message = "limit tối đa 100")
            @RequestParam(name = "limit", defaultValue = "20") int limit) {

        List<LeaderboardEntryDTO> leaderboard = leaderboardService.getLeaderboard(limit);
        return ResponseEntity.ok(leaderboard);
    }
}
