package com.datn.engflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
/**
 * class AiPromptService.
 */
public class AiPromptService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public AiPromptService(
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.base-url}") String baseUrl,
            @Value("${openrouter.model}") String model,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<String> generateSpeakingPrompt(String topic, String level) {
        String promptText = String.format(
                "Tạo một bài tập luyện nói tiếng Anh với chủ đề: \"%s\" ở trình độ %s. " +
                "Trả về JSON với các trường: title (tên bài tập), description (mô tả), prompt (hướng dẫn bằng tiếng Anh), referenceText (đoạn văn mẫu, rỗng nếu không phù hợp), category (chủ đề). " +
                "Chỉ trả về JSON, không dùng markdown.",
                topic, level
        );
        return callOpenRouter(promptText);
    }

    /**
     * Extracts the human-readable reference text from a raw AI response.
     * Small local models often wrap JSON in markdown fences or emit prose;
     * this strips fences, prefers the JSON {@code referenceText} (then
     * {@code prompt}) field, and falls back to the cleaned raw text.
     */
    public String extractReferenceText(String rawAiOutput) {
        String clean = stripMarkdownFences(rawAiOutput);
        try {
            JsonNode node = objectMapper.readTree(clean);
            String referenceText = node.path("referenceText").asText("");
            if (!referenceText.isBlank()) {
                return referenceText;
            }
            String promptField = node.path("prompt").asText("");
            if (!promptField.isBlank()) {
                return promptField;
            }
            log.info("AI JSON output missing referenceText/prompt fields; using raw text");
        } catch (Exception e) {
            log.info("AI output was not JSON; using cleaned raw text as reference");
        }
        return clean;
    }

    private static String stripMarkdownFences(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline >= 0) {
                t = t.substring(firstNewline + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
            t = t.trim();
        }
        return t;
    }

    private Mono<String> callOpenRouter(String prompt) {
        var body = java.util.Map.of(
                "model", model,
                "messages", java.util.List.of(java.util.Map.of("role", "user", "content", prompt)),
                "max_tokens", 1000
        );
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        return root.path("choices").get(0).path("message").path("content").asText();
                    } catch (Exception e) {
                        log.error("Failed to parse OpenRouter response: {}", e.getMessage());
                        return null;
                    }
                });
    }
}
