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

    /**
     * Normalises the configured endpoint and stores the model name and timeout.
     *
     * <p>A blank base URL is stored as {@code null}, which is what
     * {@link #isConfigured()} reports on. Trailing slashes are stripped so the
     * fixed {@code /chat/completions} suffix never doubles up.</p>
     *
     * @param baseUrl       Ollama-compatible root URL, or blank to disable the client
     * @param model         model name to request
     * @param timeoutSeconds per-request timeout in seconds
     * @param objectMapper  shared mapper used to build the request and read the response
     */
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

    /**
     * Reports whether an endpoint was configured.
     *
     * <p>Callers use this to degrade gracefully rather than catching the exception
     * thrown by {@link #complete}.</p>
     *
     * @return {@code true} when a usable base URL is present
     */
    public boolean isConfigured() {
        return baseUrl != null;
    }

    /**
     * Returns the model name this client requests.
     *
     * <p>Surfaces in the shadowing and subtitle features so a learner or admin can
     * see which model produced a piece of generated text.</p>
     *
     * @return the configured model name
     */
    public String getModel() {
        return model;
    }

    /**
     * Sends one chat completion and returns the assistant text.
     *
     * <p>Temperature is pinned to 0.1 for determinism. The reply must be
     * non-blank, so an empty completion is treated as a failure rather than
     * returned as an empty string.</p>
     *
     * @param systemPrompt instructions sent as the {@code system} message
     * @param userPrompt   the user turn
     * @return the assistant message content
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
     *
     * <p>Only one retry is attempted: two failures in a row mean the model is
     * genuinely unavailable, and retrying further would hold a request thread
     * for minutes.</p>
     *
     * @param systemPrompt instructions sent as the {@code system} message
     * @param userPrompt   the user turn
     * @return the assistant message content from the successful attempt
     * @throws IllegalStateException if both attempts fail
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
