package com.datn.engflow.service;

import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.repository.VocabularyRepository;
import com.datn.engflow.model.enums.DeckSource;
import com.fasterxml.jackson.core.json.JsonReadFeature;
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
 * AI vocabulary generation: asks the configured chat-completions backend for a
 * batch of words on a topic or for the detail of a single word, then maps the
 * reply onto {@link Vocabulary} rows for the vocabulary admin screens. Also
 * persists approved batches. Every reply from the small local model goes
 * through two defences first — a lenient JSON reader and a CJK gloss guard —
 * because that model intermittently returns malformed JSON and writes
 * Vietnamese definitions in Chinese.
 */
public class AiVocabService {

    private final WebClient webClient;
    private final VocabularyRepository vocabularyRepository;
    private final ObjectMapper objectMapper;
    private final String model;
    private final long timeoutSeconds;

    /**
     * audit-v17 F-17-11: a lenient reader for model output.
     *
     * <p>The local model (qwen2.5:1.5b) intermittently emits a raw control character — in
     * particular a literal newline — inside a JSON string value (e.g. a multi-line
     * {@code exampleSentence}). Strict JSON forbids unescaped control characters, so
     * {@code ObjectMapper.readValue} throws
     * {@code Illegal unquoted character ((CTRL-CHAR, code 10))} and the endpoint returned
     * HTTP 500 — measured live 2026-09-26 (round 2 of audit-v17). It is intermittent:
     * 3 subsequent calls returned 200, so a single strict parse is not safe.
     *
     * <p>ALLOW_UNESCAPED_CONTROL_CHARS accepts exactly that class of malformed-but-recoverable
     * output, which is the same failure family the exercise pipeline already salvages in
     * {@code AiExerciseService}. The strict {@link #objectMapper} is kept for the first
     * attempt so genuinely invalid JSON still fails loudly.
     */
    private static final ObjectMapper LENIENT_MAPPER = new ObjectMapper()
            .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);

    /**
     * @param apiKey               bearer token for the chat-completions endpoint
     * @param baseUrl              base URL of that endpoint
     * @param model                model name to request
     * @param timeoutSeconds       per-request timeout, from
     *                             {@code ai.vocab.timeout-seconds}
     * @param objectMapper         the shared mapper, used for the strict first
     *                             parse and to serialise the request
     * @param vocabularyRepository store for {@link #saveVocabBatch}
     */
    public AiVocabService(
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.base-url}") String baseUrl,
            @Value("${openrouter.model}") String model,
            @Value("${ai.vocab.timeout-seconds:120}") long timeoutSeconds,
            ObjectMapper objectMapper,
            VocabularyRepository vocabularyRepository) {
        this.objectMapper = objectMapper;
        this.vocabularyRepository = vocabularyRepository;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Generates a batch of vocabulary cards for a topic. Items whose gloss
     * comes back in Chinese are dropped rather than returned, so a bad batch
     * shrinks instead of carrying unusable cards into the deck editor.
     *
     * @param topic     the topic to generate about
     * @param cefrLevel the level stamped on every generated card
     * @param count     how many words to ask for
     * @return a {@link Mono} emitting the accepted cards, unsaved
     * @throws RuntimeException when the reply cannot be parsed as a JSON array
     *                          of vocab objects
     */
    public Mono<List<Vocabulary>> generateVocabByTopic(String topic, String cefrLevel, int count) {
        String prompt = String.format(
            "Generate %d English vocabulary words for topic: \"%s\" at CEFR level %s. " +
            "Return ONLY a valid JSON array of objects. Do not include any conversational text or markdown wrappers like ```json. " +
            "Each object must have exactly these keys: " +
            "word (an English word), pronunciation (IPA format), wordType (in English), " +
            "definitionEn (the definition written in ENGLISH), " +
            "definitionVi (the definition written in VIETNAMESE / tiếng Việt), " +
            "exampleSentence (one sentence in ENGLISH). " +
            "STRICT LANGUAGE RULES: definitionVi MUST be Vietnamese text in the Latin script. " +
            "NO Chinese characters (汉字) anywhere. NO other language. " +
            "If you cannot write Vietnamese, write the English definition instead — never Chinese.",
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

                List<Map<String, String>> list = readVocabListLenient(cleanJson);
                List<Vocabulary> vocabs = new ArrayList<>();
                for (Map<String, String> map : list) {
                    // audit-v19 W1: the local model (qwen2.5:1.5b, zh-centric) intermittently writes
                    // the definitionVi gloss in Chinese (measured: 鱼/雨水/天气). The prompt now forbids
                    // it, but a 1.5B model is not reliable, so drop any item whose fields contain CJK
                    // rather than persist a bad card. See AiVocabServiceLanguageGuardTest.
                    if (containsCjk(map.get("definitionVi")) || containsCjk(map.get("definitionEn"))) {
                        log.warn("Dropping AI vocab '{}': CJK characters in a definition field", map.get("word"));
                        continue;
                    }
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

    /**
     * Parse a JSON array of vocab objects, tolerating unescaped control characters in
     * string values (audit-v17 F-17-11). Strict first, lenient second — so a genuinely
     * broken payload still surfaces, but the common local-model newline-inside-string
     * case is recovered instead of throwing 500.
     */
    // package-private (not private) so AiVocabServiceLenientParseTest can exercise them directly.
    static List<Map<String, String>> readVocabListLenient(String json) throws Exception {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<List<Map<String, String>>>(){});
        } catch (com.fasterxml.jackson.core.JsonProcessingException strict) {
            return LENIENT_MAPPER.readValue(json, new TypeReference<List<Map<String, String>>>(){});
        }
    }

    /** Object variant of {@link #readVocabListLenient} for {@code enrichWord}. */
    static Map<String, String> readVocabMapLenient(String json) throws Exception {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<Map<String, String>>(){});
        } catch (com.fasterxml.jackson.core.JsonProcessingException strict) {
            return LENIENT_MAPPER.readValue(json, new TypeReference<Map<String, String>>(){});
        }
    }

    /**
     * audit-v19 W1: does the text contain CJK (Han) characters? The local model
     * ({@code qwen2.5:1.5b}) is Chinese-centric and intermittently writes the Vietnamese
     * definition slot in Chinese (measured live 2026-09-27: 鱼, 雨水, 天气, 气候). The prompt
     * now forbids it, but a 1.5B model is not reliable, so this is the deterministic guard.
     *
     * <p>Covers the CJK Unified Ideographs block (U+4E00–U+9FFF) and the Extension A block
     * (U+3400–U+4DBF). Deliberately narrow: it must NOT reject Vietnamese diacritics (Latin
     * Extended Additional, U+1EA0–U+1EFF), which are legitimate in {@code definitionVi}.
     */
    static boolean containsCjk(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if ((cp >= 0x4E00 && cp <= 0x9FFF) || (cp >= 0x3400 && cp <= 0x4DBF)) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    /**
     * Generates the full detail of one word. Unlike the batch path a bad gloss
     * is fatal here — there is nothing to drop it in favour of, so the CJK
     * case surfaces as a 400 telling the admin to retry.
     *
     * @param word the English word to describe
     * @return a {@link Mono} emitting the unsaved card
     * @throws com.datn.engflow.exception.BadRequestException when a definition
     *         field comes back containing CJK characters
     * @throws RuntimeException                                when the reply
     *         cannot be parsed as a vocab object
     */
    public Mono<Vocabulary> enrichWord(String word) {
        String prompt = String.format(
            "Provide detailed vocabulary information for the English word: \"%s\". " +
            "Return ONLY a valid JSON object. Do not include any conversational text or markdown wrappers like ```json. " +
            "The object must have exactly these keys: " +
            "word (an English word), pronunciation (IPA format), wordType (in English), " +
            "definitionEn (the definition written in ENGLISH), " +
            "definitionVi (the definition written in VIETNAMESE / tiếng Việt), " +
            "exampleSentence (one sentence in ENGLISH). " +
            "STRICT LANGUAGE RULES: definitionVi MUST be Vietnamese text in the Latin script. " +
            "NO Chinese characters (汉字) anywhere. NO other language. " +
            "If you cannot write Vietnamese, write the English definition instead — never Chinese.",
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

                Map<String, String> map = readVocabMapLenient(cleanJson);
                // audit-v19 W1: reject a Chinese gloss from the local model (see generateVocabByTopic).
                // Throw a 400 (BadRequestException) rather than a bare exception so the client gets a
                // clear, actionable message instead of an opaque 500 — this is a model-quality
                // outcome, not a server fault.
                if (containsCjk(map.get("definitionVi")) || containsCjk(map.get("definitionEn"))) {
                    log.warn("Rejecting enrich-word '{}': CJK characters in a definition field", word);
                    throw new com.datn.engflow.exception.BadRequestException(
                            "AI trả về nghĩa sai ngôn ngữ cho từ này. Vui lòng thử lại.");
                }
                return Vocabulary.builder()
                        .word(map.get("word"))
                        .pronunciation(map.get("pronunciation"))
                        .wordType(map.get("wordType"))
                        .definitionEn(map.get("definitionEn"))
                        .meaning(map.get("definitionVi"))
                        .exampleSentence(map.get("exampleSentence"))
                        .source(DeckSource.USER_CREATED.name())
                        .build();
            } catch (com.datn.engflow.exception.BadRequestException e) {
                throw e;   // audit-v19 W1: keep the 400 contract (do not re-wrap as 500)
            } catch (Exception e) {
                log.error("Failed to parse AI response: {}", e.getMessage());
                throw new RuntimeException("Failed to enrich vocabulary");
            }
        });
    }

    /**
     * Single chat-completions round trip with a shared, configurable timeout.
     * HTTP failures are translated into a {@link RuntimeException} carrying a
     * message written for the end user, because the raw status alone tells an
     * admin nothing about whether to wait, pay, or fix the config.
     *
     * @param prompt the user message to send
     * @return a {@link Mono} emitting the assistant message content
     */
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
                // audit-v17 F-17-12: was a hardcoded 30s while every other AI path is
                // configurable (speaking 120s, exercise configurable). A cold local model
                // swap measured >30s, so this returned 504 on a normal cold start.
                .timeout(Duration.ofSeconds(timeoutSeconds))
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

    /**
     * Persists a reviewed batch of generated cards in one transaction, so a
     * failure part-way through leaves none of them behind.
     *
     * @param requests the reviewed cards to store
     * @return the saved entities, in request order
     */
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
