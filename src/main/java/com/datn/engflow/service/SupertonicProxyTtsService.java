package com.datn.engflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@ConditionalOnProperty(name = "ai.exercise.tts.provider", havingValue = "supertonic")
@Slf4j
/**
 * class SupertonicProxyTtsService.
 */
public class SupertonicProxyTtsService implements TtsService {

    private final String supertonicUrl;
    private final HttpClient httpClient;

    public SupertonicProxyTtsService(@Value("${ai.exercise.tts.supertonic.url:http://localhost:8001}") String supertonicUrl) {
        this.supertonicUrl = supertonicUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public byte[] synthesize(String text, String voice, String lang) {
        try {
            String requestBody = "{\"text\":\"" + escapeJson(text) + "\",\"voice\":\"" + (voice != null ? voice : "M1") + "\",\"lang\":\"" + (lang != null ? lang : "en") + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supertonicUrl + "/synthesize"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                log.info("Supertonic TTS synthesized {} bytes for text length {}", response.body().length, text.length());
                return response.body();
            }
            log.error("Supertonic TTS failed: status={}", response.statusCode());
            return null;
        } catch (Exception e) {
            log.error("Supertonic TTS error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supertonicUrl + "/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
