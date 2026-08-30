package com.datn.engflow.service.assessment;

import com.datn.engflow.model.entity.SpeakingPrompt;
import com.datn.engflow.model.entity.SpeakingSubmission;
import com.datn.engflow.service.MinioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Automatic scoring pipeline for speaking submissions using local/free services only.
 *
 * <p>Flow: audio from MinIO → transcript (Whisper sidecar if configured, otherwise the
 * learner-supplied transcript) → alignment metrics against the prompt reference text →
 * LLM rubric on the transcript. Pronunciation is never claimed from text-only signals.</p>
 */
@Service
@Slf4j
public class SpeakingAssessmentService {

    static final String PROVIDER = "LOCAL_WHISPER_LLM";

    private static final int MAX_TRANSCRIPT_CHARS = 4000;

    private final MinioService minioService;
    private final SpeakingTranscriptClient transcriptClient;
    private final SpeakingRubricClient rubricClient;
    private final ObjectMapper objectMapper;

    public SpeakingAssessmentService(
            MinioService minioService,
            @Value("${ai.speaking.whisper.base-url:}") String whisperBaseUrl,
            @Value("${ai.speaking.ollama.base-url:http://host.docker.internal:11434/v1}") String ollamaBaseUrl,
            @Value("${ai.speaking.ollama.model:qwen2.5:3b}") String ollamaModel,
            @Value("${ai.speaking.llm.timeout-seconds:60}") long llmTimeoutSeconds,
            ObjectMapper objectMapper) {
        this.minioService = minioService;
        this.transcriptClient = new SpeakingTranscriptClient(whisperBaseUrl, Duration.ofSeconds(llmTimeoutSeconds));
        this.rubricClient = new SpeakingRubricClient(ollamaBaseUrl, ollamaModel, Duration.ofSeconds(llmTimeoutSeconds), objectMapper);
        this.objectMapper = objectMapper;
    }

    /**
     * Assesses one submission end to end.
     *
     * @param submission uploaded submission awaiting assessment
     * @return outcome with transcript, alignment, and rubric (or a failure marker)
     */
    public SpeakingAssessmentOutcome assess(SpeakingSubmission submission) {
        SpeakingPrompt prompt = submission.getPrompt();
        String referenceText = prompt == null ? null : prompt.getReferenceText();

        String transcript = submission.getTranscript();
        String transcriptSource = "USER";
        if (transcript == null || transcript.isBlank()) {
            transcript = transcribeFromAudio(submission);
            transcriptSource = transcript != null ? "WHISPER" : "NONE";
        }
        if (transcript == null || transcript.isBlank()) {
            return SpeakingAssessmentOutcome.failed(PROVIDER,
                    "Chưa có transcript: bật Whisper sidecar hoặc nhập văn bản bài nói");
        }
        transcript = transcript.trim();
        if (transcript.length() > MAX_TRANSCRIPT_CHARS) {
            transcript = transcript.substring(0, MAX_TRANSCRIPT_CHARS);
        }

        TranscriptAlignmentMetrics.AlignmentResult alignment =
                TranscriptAlignmentMetrics.compute(referenceText, transcript);

        SpeakingRubricResult rubric = null;
        String error = null;
        try {
            rubric = rubricClient.score(prompt, transcript);
        } catch (Exception ex) {
            log.warn("Rubric scoring failed for submission {}: {}", submission.getId(), ex.getMessage());
            error = truncate("LLM rubric thất bại: " + ex.getMessage(), 500);
        }
        return new SpeakingAssessmentOutcome(transcript, transcriptSource, alignment, rubric, PROVIDER, error);
    }

    private String transcribeFromAudio(SpeakingSubmission submission) {
        String objectKey = submission.getMediaObjectKey();
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try (InputStream stream = minioService.getObject(objectKey).getInputStream()) {
            byte[] audio = stream.readAllBytes();
            return transcriptClient.transcribe(audio, submission.getMediaType());
        } catch (Exception ex) {
            log.warn("Transcription failed for submission {}: {}", submission.getId(), ex.getMessage());
            return null;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    /**
     * Optional Whisper-compatible STT sidecar. Disabled when no base URL is configured.
     *
     * @param whisperBaseUrl sidecar root URL, blank disables transcription
     * @param timeout        request timeout for STT and LLM calls
     */
    static final class SpeakingTranscriptClient {

        private final String baseUrl;
        private final Duration timeout;

        SpeakingTranscriptClient(String baseUrl, Duration timeout) {
            this.baseUrl = baseUrl == null || baseUrl.isBlank() ? null : baseUrl.replaceAll("/+$", "");
            this.timeout = timeout;
        }

        boolean isConfigured() {
            return baseUrl != null;
        }

        /**
         * Sends raw audio bytes to the sidecar's {@code /v1/audio/transcriptions} endpoint.
         *
         * @param audio     encoded audio bytes
         * @param mediaType MIME type reported by the browser recorder
         * @return recognized text, or {@code null} when the sidecar is unavailable
         */
        String transcribe(byte[] audio, String mediaType) {
            if (!isConfigured() || audio == null || audio.length == 0) {
                return null;
            }
            String filename = resolveFilename(mediaType);
            try {
                String boundary = "----engflow" + System.nanoTime();
                byte[] body = buildMultipart(boundary, filename, audio);
                String response = WebClient.builder()
                        .baseUrl(baseUrl)
                        .build()
                        .post()
                        .uri("/v1/audio/transcriptions")
                        .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(timeout)
                        .block();
                JsonNode node = new ObjectMapper().readTree(response);
                String text = node.path("text").asText(null);
                return text == null || text.isBlank() ? null : text;
            } catch (Exception ex) {
                log.warn("Whisper sidecar call failed: {}", ex.getMessage());
                return null;
            }
        }

        private static String resolveFilename(String mediaType) {
            String normalized = mediaType == null ? "" : mediaType.toLowerCase(Locale.ROOT);
            if (normalized.contains("wav")) return "audio.wav";
            if (normalized.contains("ogg")) return "audio.ogg";
            if (normalized.contains("mp3") || normalized.contains("mpeg")) return "audio.mp3";
            return "audio.webm";
        }

        private static byte[] buildMultipart(String boundary, String filename, byte[] audio) {
            String head = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n";
            String modelField = "\r\n--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"model\"\r\n\r\nwhisper-1\r\n";
            String tail = "--" + boundary + "--\r\n";
            byte[] headBytes = head.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] modelBytes = modelField.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] tailBytes = tail.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] result = new byte[headBytes.length + audio.length + modelBytes.length + tailBytes.length];
            int pos = 0;
            System.arraycopy(headBytes, 0, result, pos, headBytes.length); pos += headBytes.length;
            System.arraycopy(audio, 0, result, pos, audio.length); pos += audio.length;
            System.arraycopy(modelBytes, 0, result, pos, modelBytes.length); pos += modelBytes.length;
            System.arraycopy(tailBytes, 0, result, pos, tailBytes.length);
            return result;
        }
    }

    /**
     * LLM rubric scorer over an OpenAI-compatible chat endpoint (Ollama by default).
     *
     * @param baseUrl      chat-completions root URL
     * @param model        model name to request
     * @param timeout      request timeout
     * @param objectMapper shared Jackson mapper for request bodies
     */
    static final class SpeakingRubricClient {

        private static final String SYSTEM_PROMPT = """
                You are an English speaking coach for Vietnamese learners. Grade the learner's
                transcript ONLY on language quality. You cannot hear the audio, so never comment
                on pronunciation or accent. Reply with JSON only, no markdown, exactly these keys:
                grammar (integer 0-10), vocabulary (integer 0-10), fluency (integer 0-10),
                feedback (a genuine 1-3 sentence comment in Vietnamese about the learner's English,
                written by you — never echo the field description or use angle brackets).
                Example shape: {"grammar":7,"vocabulary":6,"fluency":8,"feedback":"Bạn dùng thì hiện tại đơn khá chính xác."}
                """;

        private final String baseUrl;
        private final String model;
        private final Duration timeout;
        private final ObjectMapper objectMapper;

        SpeakingRubricClient(String baseUrl, String model, Duration timeout, ObjectMapper objectMapper) {
            this.baseUrl = baseUrl == null || baseUrl.isBlank() ? null : baseUrl.replaceAll("/+$", "");
            this.model = model;
            this.timeout = timeout;
            this.objectMapper = objectMapper;
        }

        SpeakingRubricResult score(SpeakingPrompt prompt, String transcript) {
            if (baseUrl == null) {
                throw new IllegalStateException("Ollama base URL chưa cấu hình");
            }
            String userPrompt = buildUserPrompt(prompt, transcript);
            try {
                String requestBody = objectMapper.writeValueAsString(Map.of(
                        "model", model,
                        "messages", java.util.List.of(
                                Map.of("role", "system", "content", SYSTEM_PROMPT),
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
                return parseRubric(content.asText(""));
            } catch (IllegalStateException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IllegalStateException("LLM trả kết quả không hợp lệ", ex);
            }
        }

        private static String buildUserPrompt(SpeakingPrompt prompt, String transcript) {
            StringBuilder sb = new StringBuilder();
            if (prompt != null) {
                sb.append("Task: ").append(prompt.getPrompt() == null ? prompt.getTitle() : prompt.getPrompt()).append('\n');
                if (prompt.getReferenceText() != null && !prompt.getReferenceText().isBlank()) {
                    sb.append("Reference script: ").append(prompt.getReferenceText()).append('\n');
                }
            }
            sb.append("Learner transcript: ").append(transcript);
            return sb.toString();
        }

        static SpeakingRubricResult parseRubric(String raw) {
            String json = extractJsonObject(raw);
            try {
                JsonNode node = new ObjectMapper().readTree(json);
                int grammar = clamp(node.path("grammar").asInt(0));
                int vocabulary = clamp(node.path("vocabulary").asInt(0));
                int fluency = clamp(node.path("fluency").asInt(0));
                String feedback = node.path("feedback").asText("").trim();
                if (feedback.isEmpty()) {
                    feedback = "AI chưa tạo nhận xét chi tiết cho bài này.";
                }
                return new SpeakingRubricResult(grammar, vocabulary, fluency, feedback);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Rubric JSON không hợp lệ: " + raw, ex);
            }
        }

        private static String extractJsonObject(String raw) {
            if (raw == null) {
                throw new IllegalArgumentException("empty rubric");
            }
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start < 0 || end <= start) {
                throw new IllegalArgumentException("no JSON object in rubric output");
            }
            return raw.substring(start, end + 1);
        }

        private static int clamp(int value) {
            return Math.max(0, Math.min(10, value));
        }
    }
}
