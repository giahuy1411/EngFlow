package com.datn.engflow.controller;

import com.datn.engflow.model.dto.response.ProgressResponse;
import com.datn.engflow.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
/**
 * class ProgressController.
 */
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/progress")
    public ResponseEntity<ProgressResponse> getProgressSummary(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        ProgressResponse response = progressService.getProgressSummary(email);
        return ResponseEntity.ok(response);
    }
}
