package com.datn.engflow.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression test for audit-v17 F-17-11.
 *
 * <p>Measured live 2026-09-26 (audit-v17 round 2): {@code POST /api/ai/generate-vocab}
 * returned HTTP 500 because the local model (qwen2.5:1.5b) intermittently emitted a raw
 * newline inside a JSON string value. The strict parse in {@code AiVocabService} threw
 * {@code Illegal unquoted character ((CTRL-CHAR, code 10))} and the exception propagated
 * as a 500. The failure is intermittent (3 follow-up calls returned 200), so it needs a
 * parser-level fix, not a retry.
 *
 * <p>{@code readVocabListLenient} / {@code readVocabMapLenient} try strict first, then a
 * lenient reader that tolerates unescaped control characters.
 */
class AiVocabServiceLenientParseTest {

    @Test
    @DisplayName("well-formed JSON still parses (strict path)")
    void wellFormedParses() throws Exception {
        String json = "[{\"word\":\"travel\",\"definitionEn\":\"to go on a trip\"}]";
        List<Map<String, String>> out = AiVocabService.readVocabListLenient(json);
        assertEquals(1, out.size());
        assertEquals("travel", out.get(0).get("word"));
    }

    @Test
    @DisplayName("unescaped newline inside a string value is recovered (the F-17-11 500)")
    void unescapedNewlineRecovered() throws Exception {
        // The exact malformation the model produced: a literal newline in exampleSentence.
        String json = "[{\"word\":\"travel\",\"exampleSentence\":\"I plan to travel\nto Europe.\"}]";
        List<Map<String, String>> out = AiVocabService.readVocabListLenient(json);
        assertNotNull(out);
        assertEquals(1, out.size());
        assertEquals("travel", out.get(0).get("word"));
        assertEquals("I plan to travel\nto Europe.", out.get(0).get("exampleSentence"));
    }

    @Test
    @DisplayName("unescaped tab inside a string value is recovered")
    void unescapedTabRecovered() throws Exception {
        String json = "[{\"word\":\"tab\",\"definitionEn\":\"a\tgap\"}]";
        List<Map<String, String>> out = AiVocabService.readVocabListLenient(json);
        assertEquals("tab", out.get(0).get("word"));
    }

    @Test
    @DisplayName("object variant recovers an unescaped newline too (enrichWord)")
    void objectVariantRecoversNewline() throws Exception {
        String json = "{\"word\":\"travel\",\"exampleSentence\":\"line one\nline two\"}";
        Map<String, String> out = AiVocabService.readVocabMapLenient(json);
        assertNotNull(out);
        assertEquals("travel", out.get("word"));
        assertEquals("line one\nline two", out.get("exampleSentence"));
    }

    @Test
    @DisplayName("genuinely broken JSON still fails loudly (no silent swallow)")
    void trulyBrokenJsonStillThrows() {
        String json = "[{this is not json at all";
        boolean threw = false;
        try {
            AiVocabService.readVocabListLenient(json);
        } catch (Exception e) {
            threw = true;
        }
        assertEquals(true, threw, "a payload that is not JSON must still throw, not return junk");
    }
}
