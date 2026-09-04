package com.datn.engflow.service;

import com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine;
import com.datn.engflow.service.assessment.LlmChatClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI subtitle translation for video lessons: fills {@code textVi} from
 * {@code textEn} line by line with one local LLM call (single batched request
 * so a whole transcript costs one model run, not one per line).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubtitleTranslationService {

    private static final String SYSTEM_PROMPT = """
            You are a professional English-Vietnamese subtitle translator.
            Translate each numbered English subtitle line to natural, concise spoken Vietnamese.
            Keep the same tone (casual dialogue stays casual). Keep names and numbers.
            Reply with ONLY a JSON array of strings, one translated line per input line,
            in the same order. No markdown, no commentary.
            Example input: ["Hello, how are you?", "I'm fine, thanks."]
            Example output: ["Chào bạn, bạn khỏe không?", "Mình ổn, cảm ơn."]
            """;

    private static final int MAX_LINES_PER_BATCH = 120;
    // audit-v6 F30: one 120-line batch on qwen2.5:1.5b can exceed the 180s LLM
    // timeout (measured: a full music-video transcript timed out twice).
    // Translate in smaller chunks so each call stays well inside the budget;
    // a failed chunk keeps its lines untranslated (fail-soft) instead of
    // losing the whole transcript.
    private static final int CHUNK_SIZE = 30;

    private final LlmChatClient llm;
    private final ObjectMapper objectMapper;

    /**
     * Translates the English lines of a transcript, returning lines with
     * {@code textVi} filled in. Lines already having a Vietnamese translation
     * are kept as-is. On total LLM failure the original lines are returned
     * unchanged (fail-soft — the admin can retry or translate by hand).
     */
    public List<TranscriptLine> translate(List<TranscriptLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return lines;
        }
        List<Integer> pending = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            TranscriptLine line = lines.get(i);
            boolean needsTranslation = line != null
                    && line.textEn() != null && !line.textEn().isBlank()
                    && (line.textVi() == null || line.textVi().isBlank());
            if (needsTranslation) {
                pending.add(i);
            }
        }
        if (pending.isEmpty()) {
            return lines;
        }

        List<String> translations = translateBatchChunked(indexLines(lines, pending));
        if (translations == null) {
            return lines;
        }

        List<TranscriptLine> result = new ArrayList<>(lines);
        int mapped = Math.min(translations.size(), pending.size());
        for (int k = 0; k < mapped; k++) {
            String vi = translations.get(k) == null ? "" : translations.get(k).trim();
            if (!vi.isEmpty()) {
                TranscriptLine original = result.get(pending.get(k));
                result.set(pending.get(k), new TranscriptLine(original.start(), original.end(), original.textEn(), vi));
            }
        }
        return result;
    }

    private List<String> indexLines(List<TranscriptLine> lines, List<Integer> indexes) {
        List<String> texts = new ArrayList<>();
        for (int idx : indexes) {
            texts.add(lines.get(idx).textEn());
        }
        if (texts.size() > MAX_LINES_PER_BATCH) {
            log.warn("Transcript has {} untranslated lines; truncating batch to {}", texts.size(), MAX_LINES_PER_BATCH);
            return texts.subList(0, MAX_LINES_PER_BATCH);
        }
        return texts;
    }

    /**
     * Translates in {@link #CHUNK_SIZE} chunks (audit-v6 F30). Returns a list
     * aligned with {@code texts}; a failed chunk yields nulls for its slice so
     * the caller keeps those lines untranslated.
     */
    private List<String> translateBatchChunked(List<String> texts) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += CHUNK_SIZE) {
            List<String> chunk = new ArrayList<>(texts.subList(i, Math.min(i + CHUNK_SIZE, texts.size())));
            List<String> translated = translateBatch(chunk);
            if (translated == null || translated.isEmpty()) {
                for (int k = 0; k < chunk.size(); k++) out.add(null);
            } else {
                // audit-v6 F30: a chunk may return fewer lines than requested;
                // pad with nulls so alignment with `pending` is preserved.
                for (int k = 0; k < chunk.size(); k++) {
                    out.add(k < translated.size() ? translated.get(k) : null);
                }
            }
        }
        return out;
    }

    /**
     * @return translated strings aligned with the input, or {@code null} on failure
     */
    private List<String> translateBatch(List<String> texts) {
        StringBuilder numbered = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            numbered.append(i + 1).append(". ").append(texts.get(i)).append('\n');
        }
        String userPrompt = "Translate these " + texts.size() + " subtitle lines:\n" + numbered;
        try {
            String raw = llm.completeWithRetry(SYSTEM_PROMPT, userPrompt);
            int start = raw.indexOf('[');
            int end = raw.lastIndexOf(']');
            if (start < 0 || end <= start) {
                log.warn("Translation output has no JSON array");
                return null;
            }
            List<String> parsed = objectMapper.readValue(raw.substring(start, end + 1), new TypeReference<List<String>>() {
            });
            if (parsed.size() < texts.size()) {
                log.warn("Translation returned {}/{} lines", parsed.size(), texts.size());
            }
            return parsed;
        } catch (Exception ex) {
            log.warn("Subtitle translation failed: {}", ex.getMessage());
            return null;
        }
    }
}
