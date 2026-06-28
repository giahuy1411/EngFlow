package com.datn.engflow.controller;

import com.datn.engflow.model.entity.Achievement;
import com.datn.engflow.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    public ResponseEntity<List<Achievement>> getAllAchievements() {
        return ResponseEntity.ok(achievementService.getAllAchievements());
    }

    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyAchievements(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        List<Achievement> all = achievementService.getAllAchievements();
        List<Long> unlockedIds = achievementService.getUnlockedAchievementIds(email);

        List<Map<String, Object>> result = all.stream().map(a -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", a.getId());
            entry.put("name", a.getName());
            entry.put("description", a.getDescription());
            entry.put("iconUrl", a.getIconUrl());
            entry.put("badgeType", a.getBadgeType());
            entry.put("pointsRequired", a.getPointsRequired());
            entry.put("unlocked", unlockedIds.contains(a.getId()));
            return entry;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
