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
 * Generates speaking-practice prompts with the configured chat-completions
 * backend (OpenRouter, or the local Ollama that {@code openrouter.base-url}
 * points at by default). Serves the admin "generate by AI" button through
 * {@link #generateFullPrompt} and the smaller {@link #generateSpeakingPrompt}
 * variant. Every output goes through the same salvage path, because a small
 * local model answers with prose or fenced JSON about as often as with clean
 * JSON.
 */
public class AiPromptService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    /**
     * @param apiKey       bearer token for the chat-completions endpoint
     * @param baseUrl      base URL of that endpoint
     * @param model        model name to request
     * @param objectMapper the shared mapper used to read the response
     */
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

    /**
     * Asks the model for a single speaking prompt, in Vietnamese, and returns
     * the raw model text — the caller parses it.
     *
     * @param topic the practice topic
     * @param level the learner's level
     * @return a {@link Mono} emitting the raw completion text
     */
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
     * Full speaking-prompt draft for the admin "tạo đề bằng AI" button:
     * one LLM call returns JSON with every field the admin form needs.
     * The call blocks, so a slow local model shows up as a long admin request
     * rather than a reactive pipeline the caller has to thread.
     *
     * @param topic the practice topic
     * @param level the CEFR level, or null to fall back to A2
     * @param mode  the prompt mode; "READ_ALOUD" asks for a sample paragraph,
     *              anything else asks for free speaking
     * @return parsed map of title/description/prompt/referenceText/level,
     *         with missing fields left out
     */
    public java.util.Map<String, String> generateFullPrompt(String topic, String level, String mode) {
        boolean readAloud = "READ_ALOUD".equals(mode);
        String promptText = String.format(
                "Create a NEW English speaking exercise for Vietnamese learners.\n"
                + "Topic: \"%s\". Level: %s. Mode: %s.\n"
                + "Return EXACTLY one JSON object with these string fields:\n"
                + "- \"title\": short English title of the exercise\n"
                + "- \"description\": one sentence in ENGLISH\n"
                + "- \"prompt\": instruction for the student in ENGLISH (1-2 sentences)\n"
                + "- \"referenceText\": %s\n"
                + "- \"category\": one English word (e.g. daily, travel, work)\n\n"
                + "STRICT RULES:\n"
                + "- ALL fields must be in ENGLISH only. NO Vietnamese, NO Chinese characters.\n"
                + "- Write NEW content about the topic; do not copy the example.\n"
                + "- Output ONLY the JSON object. No markdown, no commentary.",
                topic,
                level == null || level.isBlank() ? "A2" : level,
                readAloud ? "student reads a sample paragraph aloud" : "student speaks freely about the topic",
                readAloud
                        ? "a 3-5 sentence English paragraph about the topic at the given level, for the student to read aloud"
                        : "an empty string \"\" (free speaking mode needs no sample text)"
        );
        String raw = callOpenRouter(promptText)
                .timeout(java.time.Duration.ofSeconds(90))
                .block(java.time.Duration.ofSeconds(95));
        return parseFullPrompt(raw, topic, level);
    }

    /**
     * Parses the LLM JSON into the admin form's fields, always seeding topic
     * and level from the request so a failed or partial reply still yields a
     * usable draft. Non-JSON output is not thrown away — it becomes the
     * reference text.
     *
     * @param raw           the raw completion text
     * @param fallbackTopic topic to seed the result with
     * @param fallbackLevel level to seed the result with
     * @return the form fields, never null
     */
    java.util.Map<String, String> parseFullPrompt(String raw, String fallbackTopic, String fallbackLevel) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("topic", fallbackTopic == null ? "" : fallbackTopic);
        result.put("level", fallbackLevel == null ? "A2" : fallbackLevel);
        if (raw == null || raw.isBlank()) {
            return result;
        }
        String clean = stripMarkdownFences(raw);
        try {
            JsonNode node = objectMapper.readTree(clean);
            for (String field : java.util.List.of("title", "description", "prompt", "referenceText", "category", "level")) {
                String value = node.path(field).asText("").trim();
                if (!value.isEmpty()) {
                    result.put(field, value);
                }
            }
        } catch (Exception ex) {
            // Non-JSON output: use the cleaned text as referenceText so nothing is lost.
            log.info("Full prompt output not JSON; using raw text as referenceText");
            result.put("referenceText", clean);
        }
        return result;
    }

    /**
     * Extracts the human-readable reference text from a raw AI response.
     * Small local models often wrap JSON in markdown fences or emit prose;
     * this strips fences, prefers the JSON {@code referenceText} (then
     * {@code prompt}) field, and falls back to the cleaned raw text.
     *
     * @param rawAiOutput the raw completion text
     * @return the reference text to show the learner, never null for
     *         non-null input
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

    /**
     * Removes a leading {@code ```} fence and its closing counterpart, which
     * models add even when told not to. Only strips a fence that opens the
     * text, so a fence appearing mid-answer is left alone.
     *
     * @param text the raw text
     * @return the trimmed text without its outer fence
     */
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

    /**
     * Single chat-completions round trip, unwrapped from the
     * {@code choices[0].message.content} envelope.
     *
     * @param prompt the user message to send
     * @return a {@link Mono} emitting the assistant message content, or null
     *         when the envelope cannot be parsed
     */
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
