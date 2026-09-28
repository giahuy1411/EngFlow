package com.datn.engflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Đường TTS duy nhất của dự án: sidecar supertonic trong docker compose.
// (Class CloudTtsService "Z.ai" cũ đã xóa — nó không có key, không bao giờ chạy.)
@Service
@Slf4j
/**
 * The one and only {@link TtsService} implementation: an HTTP proxy to the
 * supertonic sidecar that runs in the local docker compose stack. Used by
 * {@link AiExerciseService} to render the spoken prompt of a generated
 * LISTENING exercise, and by the health probe before every synthesis.
 */
public class SupertonicProxyTtsService implements TtsService {

    private final String supertonicUrl;
    private final HttpClient httpClient;

    /**
     * @param supertonicUrl base URL of the sidecar, from
     *                      {@code ai.exercise.tts.supertonic.url}
     */
    public SupertonicProxyTtsService(@Value("${ai.exercise.tts.supertonic.url:http://localhost:8001}") String supertonicUrl) {
        this.supertonicUrl = supertonicUrl;
        // audit-v5 TTS: force HTTP/1.1. Java HttpClient defaults to HTTP/2 and
        // sends an h2c Upgrade handshake; the uvicorn sidecar only speaks
        // HTTP/1.1, and the downgrade drops the POST body -> FastAPI sees an
        // empty body and rejects with 422 "Field required". HTTP/1.1 fixes it.
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * POSTs a {@code /synthesize} request to the sidecar and returns the raw
     * audio bytes. Fails soft: any non-200 status or transport error is logged
     * and yields null so the caller can ship an exercise without audio.
     *
     * @param text  the text to speak
     * @param voice voice id, or null for the sidecar default
     * @param lang  language hint, or null for the sidecar default
     * @return the audio bytes, or null on failure
     */
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

    /**
     * Health probe against the sidecar's {@code /health} endpoint. Used by
     * {@link AiExerciseService} to skip TTS entirely when the sidecar is down
     * rather than burn a synthesis request on a guaranteed failure.
     *
     * @return true when the sidecar answers 200 within 3 seconds
     */
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

    /**
     * Minimal hand-rolled JSON string escaper — the prompt is the only
     * untrusted part of the request body and the sidecar accepts a flat
     * object, so a full serializer would be overkill.
     *
     * @param text raw text that may contain quotes, backslashes or newlines
     * @return the text safe to embed between JSON double quotes
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
