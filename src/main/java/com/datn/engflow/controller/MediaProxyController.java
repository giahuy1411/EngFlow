package com.datn.engflow.controller;

import com.datn.engflow.repository.SpeakingSubmissionRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.VideoAttemptRepository;
import com.datn.engflow.security.MediaSigner;
import com.datn.engflow.service.MinioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
/**
 * class MediaProxyController.
 */
public class MediaProxyController {

    private final MinioService minioService;
    private final MediaSigner mediaSigner;
    private final SpeakingSubmissionRepository submissionRepository;
    private final VideoAttemptRepository videoAttemptRepository;
    private final UserRepository userRepository;

    @GetMapping("/api/v1/media/**")
    public ResponseEntity<InputStreamResource> serveMedia(HttpServletRequest request) {
        String path = request.getRequestURI();
        String prefix = "/api/v1/media/";
        if (!path.startsWith(prefix)) {
            return ResponseEntity.notFound().build();
        }
        String objectKey = path.substring(prefix.length()).replaceFirst("^/+", "");
        if (objectKey.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // audit-v7 F55: learner recordings are personal data; a bare object key
        // used to be a public download URL (IDOR). Two acceptable proofs now:
        //  1. server-signed expiring ticket (?exp=&sig=) — the only form the
        //     UI ever generates for owners/admins, works in <audio src>;
        //  2. legacy unsaved URLs persisted before the fix: allow only when the
        //     CURRENT authenticated principal owns the object.
        boolean signed = mediaSigner.verify(objectKey, request.getParameter("exp"), request.getParameter("sig"));
        if (!signed && !ownsCurrently(objectKey)) {
            return ResponseEntity.status(403).build();
        }
        return stream(objectKey);
    }

    private boolean ownsCurrently(String objectKey) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return userRepository.findByEmail(auth.getName())
                .map(u -> submissionRepository.existsByMediaObjectKeyAndUserId(objectKey, u.getId())
                        || videoAttemptRepository.existsByMediaObjectKeyAndUserId(objectKey, u.getId()))
                .orElse(false);
    }

    private ResponseEntity<InputStreamResource> stream(String objectKey) {
        InputStreamResource resource = minioService.getObject(objectKey);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        String contentType = "application/octet-stream";
        if (objectKey.endsWith(".webm")) {
            contentType = "audio/webm";
        } else if (objectKey.endsWith(".ogg")) {
            contentType = "audio/ogg";
        } else if (objectKey.endsWith(".mp3")) {
            contentType = "audio/mpeg";
        } else if (objectKey.endsWith(".wav")) {
            contentType = "audio/wav";
        } else if (objectKey.endsWith(".mp4")) {
            contentType = "video/mp4";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .body(resource);
    }
}
