package com.datn.engflow.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Minimal OpenAI-compatible chat client for small auxiliary AI tasks
 * (shadowing feedback, subtitle translation) against local Ollama.
 *
 * <p>Uses {@code qwen2.5:1.5b} by default: these tasks are short-form, and the
 * 4GB GPU holds only one loaded model — sharing the same model as exercise
 * generation avoids a costly model swap while a rubric request is in flight.</p>
 */
@Service
@Slf4j
public class LlmChatClient {

    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final ObjectMapper objectMapper;

    public LlmChatClient(
            @Value("${ai.speaking.ollama.base-url:http://host.docker.internal:11434/v1}") String baseUrl,
            @Value("${ai.exercise.ollama.model:qwen2.5:1.5b}") String model,
            @Value("${ai.speaking.llm.timeout-seconds:120}") long timeoutSeconds,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? null : baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return baseUrl != null;
    }

    public String getModel() {
        return model;
    }

    /**
     * Sends one chat completion and returns the assistant text.
     *
     * @throws IllegalStateException when unconfigured, timed out, or the model failed
     */
    public String complete(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            throw new IllegalStateException("Ollama base URL chưa cấu hình");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)),
                    "temperature", 0.1,
                    "stream", false));
            String response = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout)
                    .onErrorResume(ex -> Mono.error(new IllegalStateException("LLM không phản hồi: " + ex.getMessage())))
                    .block();
            JsonNode content = objectMapper.readTree(response).path("choices").path(0)
                    .path("message").path("content");
            String text = content.asText("");
            if (text.isBlank()) {
                throw new IllegalStateException("LLM trả về nội dung rỗng");
            }
            return text;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("LLM trả kết quả không hợp lệ", ex);
        }
    }

    /**
     * Runs {@link #complete} with one warm retry — the first attempt often
     * absorbs a cold model load (~31s on this machine) and times out.
     */
    public String completeWithRetry(String systemPrompt, String userPrompt) {
        try {
            return complete(systemPrompt, userPrompt);
        } catch (Exception first) {
            log.warn("LLM call failed ({}), retrying once", first.getMessage());
            return complete(systemPrompt, userPrompt);
        }
    }
}
