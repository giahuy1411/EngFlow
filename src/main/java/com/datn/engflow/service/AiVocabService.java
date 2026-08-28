package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.repository.VocabularyRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.model.enums.DeckSource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
/**
 * class AiVocabService.
 */
public class AiVocabService {

    private static final int AI_GENERATION_LIMIT = 5;

    private final WebClient webClient;
    private final VocabularyRepository vocabularyRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final String model;

    public AiVocabService(
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.base-url}") String baseUrl,
            @Value("${openrouter.model}") String model,
            ObjectMapper objectMapper,
            VocabularyRepository vocabularyRepository,
            UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.vocabularyRepository = vocabularyRepository;
        this.userRepository = userRepository;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Kiểm tra quota trước khi gọi AI, không tăng bộ đếm.
     */
    public boolean hasAiGenerationQuota(User user) {
        int used = user.getAiGenerationCount() == null ? 0 : user.getAiGenerationCount();
        return used < AI_GENERATION_LIMIT;
    }

    @Transactional
    public void incrementAiGenerationQuota(User user) {
        int used = user.getAiGenerationCount() == null ? 0 : user.getAiGenerationCount();
        user.setAiGenerationCount(used + 1);
        userRepository.save(user);
    }

    public Mono<List<Vocabulary>> generateVocabByTopic(String topic, String cefrLevel, int count) {
        String prompt = String.format(
            "Generate %d English vocabulary words for topic: \"%s\" at CEFR level %s. " +
            "Return ONLY a valid JSON array of objects. Do not include any conversational text or markdown wrappers like ```json. " +
            "Each object must have exactly these keys: " +
            "word, pronunciation (IPA format), wordType, definitionEn, definitionVi, exampleSentence.",
            count, topic, cefrLevel
        );

        return callOpenRouter(prompt).map(jsonArrayString -> {
            try {
                String cleanJson = jsonArrayString.trim();
                if (cleanJson.startsWith("```json")) {
                    cleanJson = cleanJson.replace("```json", "").replace("```", "").trim();
                } else if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.replace("```", "").trim();
                }

                List<Map<String, String>> list = objectMapper.readValue(cleanJson, new TypeReference<List<Map<String, String>>>(){});
                List<Vocabulary> vocabs = new ArrayList<>();
                for (Map<String, String> map : list) {
                    Vocabulary v = Vocabulary.builder()
                            .word(map.get("word"))
                            .pronunciation(map.get("pronunciation"))
                            .wordType(map.get("wordType"))
                            .definitionEn(map.get("definitionEn"))
                            .meaning(map.get("definitionVi"))
                            .exampleSentence(map.get("exampleSentence"))
                            .cefrLevel(cefrLevel)
                            .source(DeckSource.AI_GENERATED.name())
                            .build();
                    vocabs.add(v);
                }
                return vocabs;
            } catch (Exception e) {
                log.error("Failed to parse AI response: {}", e.getMessage());
                throw new RuntimeException("Failed to generate vocabulary");
            }
        });
    }

    public Mono<Vocabulary> enrichWord(String word) {
        String prompt = String.format(
            "Provide detailed vocabulary information for the English word: \"%s\". " +
            "Return ONLY a valid JSON object. Do not include any conversational text or markdown wrappers like ```json. " +
            "The object must have exactly these keys: " +
            "word, pronunciation (IPA format), wordType, definitionEn, definitionVi, exampleSentence.",
            word
        );

        return callOpenRouter(prompt).map(jsonString -> {
             try {
                String cleanJson = jsonString.trim();
                if (cleanJson.startsWith("```json")) {
                    cleanJson = cleanJson.replace("```json", "").replace("```", "").trim();
                } else if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.replace("```", "").trim();
                }

                Map<String, String> map = objectMapper.readValue(cleanJson, new TypeReference<Map<String, String>>(){});
                return Vocabulary.builder()
                        .word(map.get("word"))
                        .pronunciation(map.get("pronunciation"))
                        .wordType(map.get("wordType"))
                        .definitionEn(map.get("definitionEn"))
                        .meaning(map.get("definitionVi"))
                        .exampleSentence(map.get("exampleSentence"))
                        .source(DeckSource.USER_CREATED.name())
                        .build();
            } catch (Exception e) {
                log.error("Failed to parse AI response: {}", e.getMessage());
                throw new RuntimeException("Failed to enrich vocabulary");
            }
        });
    }

    private Mono<String> callOpenRouter(String prompt) {
        Map<String, Object> requestBodyMap = Map.of(
            "model", model,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "temperature", 0.2, // Tối ưu hóa cho JSON (giảm tính sáng tạo ngẫu hứng)
            "stream", false
        );
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(requestBodyMap);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to serialize request body", e));
        }

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    response -> response.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("OpenRouter API error: status={}, body={}", response.statusCode(), errorBody);
                            String message = switch (response.statusCode().value()) {
                                case 429 -> "AI service is temporarily busy. Please try again later.";
                                case 402 -> "AI service requires payment credits. Please add credits to your OpenRouter account.";
                                case 404 -> "AI model not found. Please check the model configuration.";
                                default -> "AI service unavailable (HTTP " + response.statusCode() + ")";
                            };
                            return Mono.error(new RuntimeException(message));
                        }))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    try {
                        JsonNode node = objectMapper.readTree(responseBody);
                        JsonNode choices = node.path("choices");
                        if (choices.isArray() && choices.size() > 0) {
                            String content = choices.get(0).path("message").path("content").asText();
                            return content;
                        }
                        log.error("Unexpected OpenRouter response: no choices. body={}", responseBody);
                        throw new RuntimeException("AI returned unexpected response");
                    } catch (Exception e) {
                        log.error("Failed to parse OpenRouter response: {}", e.getMessage());
                        throw new RuntimeException("Failed to parse AI response", e);
                    }
                })
                .doOnError(e -> log.error("OpenRouter request failed", e));
    }

    @Transactional
    public List<Vocabulary> saveVocabBatch(List<VocabularyRequest> requests) {
        List<Vocabulary> vocabularies = new ArrayList<>();
        for (VocabularyRequest req : requests) {
            Vocabulary v = Vocabulary.builder()
                    .word(req.getWord())
                    .pronunciation(req.getPronunciation())
                    .meaning(req.getMeaning())
                    .definitionEn(req.getDefinitionEn())
                    .exampleSentence(req.getExampleSentence())
                    .wordType(req.getWordType())
                    .cefrLevel(req.getCefrLevel())
                    .source("AI_GENERATED")
                    .build();
            vocabularies.add(v);
        }
        List<Vocabulary> saved = vocabularyRepository.saveAll(vocabularies);
        log.info("Saved {} AI-generated vocabulary words to DB", saved.size());
        return saved;
    }
}
