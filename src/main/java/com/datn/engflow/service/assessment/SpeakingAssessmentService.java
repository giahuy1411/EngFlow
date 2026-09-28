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
 *
 * <p>Called by {@code SpeakingSubmissionService} right after a recording is
 * uploaded, and by {@code ShadowingAiGradingService} via {@link #transcribe}.
 * Two collaborators are built in the constructor rather than injected —
 * {@link SpeakingTranscriptClient} and {@link SpeakingRubricClient} — because each
 * wraps endpoint and model settings that exist only for this pipeline. A blank
 * Whisper base URL leaves transcription disabled, and the learner-supplied text is
 * used instead.</p>
 */
@Service
@Slf4j
public class SpeakingAssessmentService {

    static final String PROVIDER = "LOCAL_WHISPER_LLM";

    // Đo được: một bài nói của học viên hiếm khi vượt 4000 ký tự (~700 từ). Cắt ở
    // đây giữ request LLM và ma trận edit-distance trong giới hạn mà không cắt
    // mất câu cuối của các bài thật.
    private static final int MAX_TRANSCRIPT_CHARS = 4000;

    private final MinioService minioService;
    private final SpeakingTranscriptClient transcriptClient;
    private final SpeakingRubricClient rubricClient;
    private final ObjectMapper objectMapper;

    /**
     * Builds the two internal clients from configuration.
     *
     * <p>Both share the {@code ai.speaking.llm.timeout-seconds} budget, since the
     * whole assessment is expected to finish inside it. The rubric client reuses
     * the shared {@link ObjectMapper} for its request and response bodies; the
     * transcript client does not, because it parses a single fixed field.</p>
     *
     * @param minioService     service used to stream the stored recording back
     * @param whisperBaseUrl   Whisper sidecar root URL, blank disables transcription
     * @param ollamaBaseUrl    chat-completions root URL for the rubric
     * @param ollamaModel      model name requested for rubric scoring
     * @param llmTimeoutSeconds shared timeout for both clients
     * @param objectMapper     shared Jackson mapper
     */
    public SpeakingAssessmentService(
            MinioService minioService,
            @Value("${ai.speaking.whisper.base-url:}") String whisperBaseUrl,
            @Value("${ai.speaking.ollama.base-url:http://host.docker.internal:11434/v1}") String ollamaBaseUrl,
            @Value("${ai.speaking.ollama.model:qwen2.5:3b}") String ollamaModel,
            @Value("${ai.speaking.llm.timeout-seconds:120}") long llmTimeoutSeconds,
            ObjectMapper objectMapper) {
        this.minioService = minioService;
        this.transcriptClient = new SpeakingTranscriptClient(whisperBaseUrl, Duration.ofSeconds(llmTimeoutSeconds));
        this.rubricClient = new SpeakingRubricClient(ollamaBaseUrl, ollamaModel, Duration.ofSeconds(llmTimeoutSeconds), objectMapper);
        this.objectMapper = objectMapper;
    }

    /**
     * Assesses one submission end to end.
     *
     * <p>A transcript already stored on the submission wins; the Whisper sidecar is
     * only called when there is none. With no transcript obtainable the method
     * returns a {@link SpeakingAssessmentOutcome#failed failed} outcome whose message
     * differs depending on whether the sidecar is configured, so the learner is told
     * whether to re-record or to type instead. The transcript is trimmed and
     * truncated to {@link #MAX_TRANSCRIPT_CHARS} before scoring. Rubric failure is
     * non-fatal: alignment metrics are still returned, with the reason in
     * {@code error}.</p>
     *
     * @param submission uploaded submission awaiting assessment
     * @return outcome with transcript, alignment, and rubric (or a failure marker)
     */
    public SpeakingAssessmentOutcome assess(SpeakingSubmission submission) {
        SpeakingPrompt prompt = submission.getPrompt();
        String referenceText = prompt == null ? null : prompt.getReferenceText();

        String transcript = submission.getTranscript();
        String transcriptSource = "USER";
        boolean usedWhisper = false;
        if (transcript == null || transcript.isBlank()) {
            transcript = transcribeFromAudio(submission);
            usedWhisper = transcript != null && !transcript.isBlank();
            transcriptSource = usedWhisper ? "WHISPER" : "NONE";
        }
        if (transcript == null || transcript.isBlank()) {
            if (transcriptClient.isConfigured()) {
                return SpeakingAssessmentOutcome.failed(PROVIDER,
                        "AI không nghe rõ transcript từ bản ghi âm. Thử ghi lại ở nơi yên tĩnh và nói to, rõ hơn.");
            }
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
            // Small local models time out when the GPU model slot was just swapped
            // (load ~31s alone). One warm retry usually succeeds; two failures in a
            // row means the model is genuinely unavailable.
            log.warn("Rubric scoring failed for submission {}: {} — retrying once", submission.getId(), ex.getMessage());
            try {
                rubric = rubricClient.score(prompt, transcript);
            } catch (Exception retryEx) {
                log.warn("Rubric retry failed for submission {}: {}", submission.getId(), retryEx.getMessage());
                error = truncate("LLM rubric thất bại: " + retryEx.getMessage(), 500);
            }
        }
        return new SpeakingAssessmentOutcome(transcript, transcriptSource, alignment, rubric, PROVIDER, error);
    }

    /**
     * Pulls the recording's object key and media type off the submission and
     * transcribes it.
     *
     * <p>A thin indirection over {@link #transcribe(String, String)} that keeps
     * the entity field names out of the pipeline body.</p>
     *
     * @param submission submission whose media should be transcribed
     * @return recognized text, or {@code null} when unavailable
     */
    private String transcribeFromAudio(SpeakingSubmission submission) {
        return transcribe(submission.getMediaObjectKey(), submission.getMediaType());
    }

    /**
     * Transcribes stored audio through the Whisper sidecar.
     *
     * <p>The recording is streamed from MinIO and read fully into memory before
     * the request is sent. Every failure mode — a missing object, an unconfigured
     * sidecar, a non-2xx response, a timeout — is logged and collapsed into a
     * {@code null} return, so callers can treat transcription as best-effort.
     * {@code ShadowingAiGradingService} also calls this directly.</p>
     *
     * @param objectKey MinIO object key of the recording
     * @param mediaType MIME type reported by the browser recorder
     * @return recognized text, or {@code null} when unavailable
     */
    public String transcribe(String objectKey, String mediaType) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try (InputStream stream = minioService.getObject(objectKey).getInputStream()) {
            byte[] audio = stream.readAllBytes();
            return transcriptClient.transcribe(audio, mediaType);
        } catch (Exception ex) {
            log.warn("Transcription failed for object {}: {}", objectKey, ex.getMessage());
            return null;
        }
    }

    /**
     * Clips a message to a maximum length.
     *
     * <p>Applied to the stored {@code error} so a long upstream failure message
     * cannot overflow the column; {@code null} passes through unchanged.</p>
     *
     * @param value text to clip, may be {@code null}
     * @param max  maximum number of characters to keep
     * @return the value, truncated to {@code max} characters
     */
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

        /**
         * Normalises the sidecar endpoint and stores the request timeout.
         *
         * @param baseUrl sidecar root URL; blank disables the client
         * @param timeout request timeout
         */
        SpeakingTranscriptClient(String baseUrl, Duration timeout) {
            this.baseUrl = baseUrl == null || baseUrl.isBlank() ? null : baseUrl.replaceAll("/+$", "");
            this.timeout = timeout;
        }

        /**
         * Reports whether the sidecar was configured.
         *
         * <p>{@link SpeakingAssessmentService#assess} reads this to choose between
         * a re-record hint and a type-the-transcript hint.</p>
         *
         * @return {@code true} when a sidecar URL is present
         */
        boolean isConfigured() {
            return baseUrl != null;
        }

        /**
         * Sends raw audio bytes to the sidecar's {@code /v1/audio/transcriptions} endpoint.
         *
         * <p>The multipart body is assembled by hand because the audio arrives as a
         * byte array, not a file. Any failure is logged and returned as
         * {@code null}, including a response whose {@code text} field is missing or
         * blank — silence is not a transcription.</p>
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
                // Sidecar trả {"text": ""} khi không nghe ra gì — coi như không có
                // transcript để assess() chọn đúng nhánh hướng dẫn cho người học.
                return text == null || text.isBlank() ? null : text;
            } catch (Exception ex) {
                log.warn("Whisper sidecar call failed: {}", ex.toString());
                return null;
            }
        }

        /**
         * Picks a filename extension matching the browser's recorded media type.
         *
         * <p>The sidecar chooses its decoder from the filename, so an unmatched
         * type falls back to {@code audio.webm} — the format MediaRecorder
         * produces by default.</p>
         *
         * @param mediaType MIME type reported by the browser recorder, may be {@code null}
         * @return a filename ending in {@code wav}, {@code ogg}, {@code mp3} or {@code webm}
         */
        private static String resolveFilename(String mediaType) {
            String normalized = mediaType == null ? "" : mediaType.toLowerCase(Locale.ROOT);
            if (normalized.contains("wav")) return "audio.wav";
            if (normalized.contains("ogg")) return "audio.ogg";
            if (normalized.contains("mp3") || normalized.contains("mpeg")) return "audio.mp3";
            return "audio.webm";
        }

        /**
         * Concatenates the multipart/form-data body by hand.
         *
         * <p>WebClient would need a file part for this, but the audio is already
         * an in-memory array. The result is the raw head, audio, extra
         * {@code model} field, and closing boundary copied into one buffer in
         * that order.</p>
         *
         * @param boundary multipart boundary string, without leading dashes
         * @param filename name reported in the {@code file} part
         * @param audio    encoded audio bytes
         * @return the complete multipart payload
         */
        private static byte[] buildMultipart(String boundary, String filename, byte[] audio) {
            String head = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n";
            // OpenAI-compatible clients send "model"; Flask/requests tolerates extra fields.
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
     * <p>The system prompt is a constant, not per-request state: it fixes the
     * output contract (four JSON keys, no markdown, no pronunciation claims) and
     * adds spelling rules for Vietnamese, because small local models drop
     * diacritics on words the learner will read. Nothing about it varies with the
     * submission.</p>
     *
     * @param baseUrl      chat-completions root URL
     * @param model        model name to request
     * @param timeout      request timeout
     * @param objectMapper shared Jackson mapper for request bodies
     */
    static final class SpeakingRubricClient {

        private static final String SYSTEM_PROMPT = """
                You are an experienced Vietnamese English teacher giving feedback to a student.

                Grade the transcript ONLY on language quality. You cannot hear the audio,
                so NEVER comment on pronunciation or accent.

                Reply with JSON only, no markdown, exactly these keys:
                grammar (integer 0-10), vocabulary (integer 0-10), fluency (integer 0-10),
                feedback (1-3 sentences in Vietnamese).

                Vietnamese writing rules — you MUST follow all of them:
                1. Common words are written exactly like this: "từ vựng", "ngữ pháp",
                   "phát âm", "trôi chảy", "câu", "bài", "đọc", "nói", "người học".
                2. NEVER write "từ vựt", "ngư pháp", "phat am", "noi chay" or any
                   word without its diacritics.
                3. If you are not 100% sure how to spell a Vietnamese word, rewrite
                   the sentence using a simpler word you are sure about.
                4. Re-read your feedback once before answering and fix any misspelled word.

                Correct example: "Bạn dùng từ vựng đơn giản nhưng chính xác."
                Wrong example:   "Bạn dùng từ vựt đơn giản nhưng chính xác."
                JSON example shape: {"grammar":7,"vocabulary":6,"fluency":8,"feedback":"Bạn dùng thì hiện tại đơn khá chính xác."}
                """;

        private final String baseUrl;
        private final String model;
        private final Duration timeout;
        private final ObjectMapper objectMapper;

        /**
         * Normalises the chat endpoint and stores model, timeout and mapper.
         *
         * @param baseUrl      chat-completions root URL; blank disables scoring
         * @param model        model name to request
         * @param timeout      request timeout
         * @param objectMapper shared Jackson mapper
         */
        SpeakingRubricClient(String baseUrl, String model, Duration timeout, ObjectMapper objectMapper) {
            this.baseUrl = baseUrl == null || baseUrl.isBlank() ? null : baseUrl.replaceAll("/+$", "");
            this.model = model;
            this.timeout = timeout;
            this.objectMapper = objectMapper;
        }

        /**
         * Asks the model to grade one transcript and parses its JSON answer.
         *
         * <p>Temperature is 0.1 for determinism. Failures surface as
         * {@link IllegalStateException} — unconfigured endpoint, timeout, empty
         * content, or unparseable output — so the caller's retry logic in
         * {@link SpeakingAssessmentService#assess} has a single thing to catch.</p>
         *
         * @param prompt     the task the learner attempted, may be {@code null}
         * @param transcript recognized or learner-supplied text
         * @return the parsed rubric with scores clamped to 0-10
         * @throws IllegalStateException when unconfigured, timed out, or the reply was unusable
         */
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

        /**
         * Builds the user turn from the task and the transcript.
         *
         * <p>The task line falls back to the prompt title when
         * {@code prompt} is null, and the reference script is included only when
         * the prompt actually has one — unscripted tasks must not present a
         * reference the learner never saw.</p>
         *
         * @param prompt     the attempted prompt, may be {@code null}
         * @param transcript recognized or learner-supplied text
         * @return the user message content
         */
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

        /**
         * Parses the model's reply into a rubric.
         *
         * <p>Scores are clamped into 0-10 so a hallucinated 15 cannot reach the
         * database, and empty feedback is replaced with a fixed sentence so the
         * learner never sees a blank comment. The Vietnamese spelling pass runs
         * before that fallback. A reply with no JSON object at all is rejected
         * rather than defaulted, since a silently zeroed score is worse than a
         * visible failure.</p>
         *
         * @param raw assistant message content, possibly wrapped in markdown
         * @return the parsed rubric
         * @throws IllegalArgumentException if no JSON object can be found or parsed
         */
        static SpeakingRubricResult parseRubric(String raw) {
            String json = extractJsonObject(raw);
            try {
                JsonNode node = new ObjectMapper().readTree(json);
                int grammar = clamp(node.path("grammar").asInt(0));
                int vocabulary = clamp(node.path("vocabulary").asInt(0));
                int fluency = clamp(node.path("fluency").asInt(0));
                String feedback = sanitizeVietnameseSpelling(node.path("feedback").asText("").trim());
                if (feedback.isEmpty()) {
                    feedback = "AI chưa tạo nhận xét chi tiết cho bài này.";
                }
                return new SpeakingRubricResult(grammar, vocabulary, fluency, feedback);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Rubric JSON không hợp lệ: " + raw, ex);
            }
        }

        /**
         * Small local models occasionally misspell common Vietnamese words in
         * learner-facing feedback. Correction is deterministic Java, not another
         * LLM call, so the fix cannot regress. Extend the map as new misspellings
         * surface in production feedback.
         *
         * <p>Replacements are applied in the map's own iteration order and use
         * plain {@code String.replace}, so a key that is a substring of another
         * entry's value is safe, but a key that is a substring of a longer
         * misspelling would win by ordering — hence each variant is listed
         * explicitly rather than derived.</p>
         *
         * @param feedback learner-facing text, may be {@code null} or empty
         * @return the text with known misspellings replaced
         */
        static String sanitizeVietnameseSpelling(String feedback) {
            if (feedback == null || feedback.isEmpty()) {
                return feedback;
            }
            String corrected = feedback;
            for (Map.Entry<String, String> misspelling : VIETNAMESE_MISSPELLINGS.entrySet()) {
                corrected = corrected.replace(misspelling.getKey(), misspelling.getValue());
            }
            return corrected;
        }

        /**
         * Misspelling to correction map applied to learner-facing feedback.
         *
         * <p>Keys include the diacritic-stripped and partially-stripped forms
         * the 1.5b-3b models actually produce, not just the fully-stripped one.
         * The order of {@code Map.ofEntries} is unspecified, which is harmless:
         * no key is a substring of another key.</p>
         */
        private static final Map<String, String> VIETNAMESE_MISSPELLINGS = Map.ofEntries(
                Map.entry("từ vựt", "từ vựng"),
                Map.entry("tư vựng", "từ vựng"),
                Map.entry("tư vựt", "từ vựng"),
                Map.entry("ngư pháp", "ngữ pháp"),
                Map.entry("ngũ pháp", "ngữ pháp"),
                Map.entry("ngu pháp", "ngữ pháp"),
                Map.entry("ngu phap", "ngữ pháp"),
                Map.entry("phat am", "phát âm"),
                Map.entry("phat âm", "phát âm"),
                Map.entry("phát am", "phát âm"),
                Map.entry("troi chay", "trôi chảy"),
                Map.entry("troi chảy", "trôi chảy"),
                Map.entry("trôi chay", "trôi chảy"),
                Map.entry("ngươi học", "người học"),
                Map.entry("nguoi hoc", "người học"),
                Map.entry("bai doc", "bài đọc"),
                Map.entry("bai đọc", "bài đọc"),
                Map.entry("bài doc", "bài đọc"),
                Map.entry("câu tru", "câu trúc"),
                Map.entry("cấu truc", "cấu trúc"),
                Map.entry("câu truc", "câu trúc"));

        /**
         * Isolates the first JSON object from a possibly chatty model reply.
         *
         * <p>Small local models sometimes wrap the JSON in a sentence or a
         * ```json fence, so the substring between the first {@code &#123;} and
         * the last {@code &#125;} is taken rather than the whole message. Using
         * first-and-last rather than first-matching-brace tolerates nested
         * objects. Text with no braces is rejected outright.</p>
         *
         * @param raw assistant message content
         * @return the JSON object substring
         * @throws IllegalArgumentException if the text is null or contains no object
         */
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

        /**
         * Constrains a model-supplied score to the 0-10 scale.
         *
         * <p>Bounds the value rather than rejecting it: an out-of-range score is
         * evidence the model answered, and clamping keeps the submission gradable.</p>
         *
         * @param value raw score from the model
         * @return the score clamped into 0-10
         */
        private static int clamp(int value) {
            return Math.max(0, Math.min(10, value));
        }
    }
}
