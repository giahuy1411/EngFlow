package com.datn.engflow.controller;

import com.datn.engflow.service.MinioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
/**
 * class MediaProxyController.
 */
public class MediaProxyController {

    private final MinioService minioService;

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
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .body(resource);
    }
}
