package com.datn.engflow.controller;

import com.datn.engflow.model.dto.response.LeaderboardEntryDTO;
import com.datn.engflow.service.LeaderboardService;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
/**
 * class LeaderboardController.
 */
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<Page<LeaderboardEntryDTO>> getLeaderboard(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Max(value = 100, message = "size tối đa 100")
            @RequestParam(name = "size", defaultValue = "20") int size,
            @Max(value = 100, message = "limit tối đa 100")
            @RequestParam(name = "limit", required = false) Integer limit) {

        int effectiveSize = limit != null ? limit : size;
        Page<LeaderboardEntryDTO> leaderboard = leaderboardService.getLeaderboard(page, effectiveSize);
        return ResponseEntity.ok(leaderboard);
    }
}
