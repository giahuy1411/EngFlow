package com.datn.engflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Primary
@ConditionalOnProperty(name = "ai.exercise.tts.provider", havingValue = "Z.ai", matchIfMissing = true)
@Slf4j
/**
 * class CloudTtsService.
 */
public class CloudTtsService implements TtsService {

    private final String apiKey;
    private final String voice;
    private final HttpClient httpClient;

    public CloudTtsService(
            @Value("${ai.exercise.tts.Z.ai.api-key:}") String apiKey,
            @Value("${ai.exercise.tts.Z.ai.voice:en-US-Neural2-C}") String voice) {
        this.apiKey = apiKey;
        this.voice = voice;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public byte[] synthesize(String text, String voiceId, String lang) {
        if (!isAvailable()) {
            log.warn("Cloud TTS not available (no API key configured)");
            return null;
        }
        try {
            String useVoice = (voiceId != null && !voiceId.isBlank()) ? voiceId : voice;
            String requestBody = "{\"input\":{\"text\":\"" + escapeJson(text) + "\"},\"voice\":{\"languageCode\":\"" + lang + "\",\"name\":\"" + useVoice + "\"},\"audioConfig\":{\"audioEncoding\":\"MP3\"}}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://texttospeech.googleapis.com/v1/text:synthesize?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String audioContent = extractField(response.body(), "audioContent");
                if (audioContent != null) {
                    return java.util.Base64.getDecoder().decode(audioContent);
                }
            }
            log.error("Cloud TTS failed: status={}", response.statusCode());
            return null;
        } catch (Exception e) {
            log.error("Cloud TTS error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }
}
