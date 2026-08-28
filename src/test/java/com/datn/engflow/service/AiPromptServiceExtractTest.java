package com.datn.engflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiPromptService#extractReferenceText(String)}.
 * Covers markdown-fenced JSON, bare JSON, and plain-prose fallbacks
 * produced by small local models such as qwen2.5.
 */
class AiPromptServiceExtractTest {

    private final AiPromptService service =
            new AiPromptService("test-key", "http://localhost", "test-model", new ObjectMapper());

    @Test
    void stripsFenceAndPicksReferenceTextField() {
        String raw = "```json\n{\"title\":\"T\",\"description\":\"D\","
                + "\"prompt\":\"Speak about your day.\","
                + "\"referenceText\":\"Yesterday I woke up at seven.\"}\n```";
        String out = service.extractReferenceText(raw);
        assertEquals("Yesterday I woke up at seven.", out);
    }

    @Test
    void fallsBackToPromptFieldWhenReferenceMissing() {
        String raw = "{\"title\":\"T\",\"prompt\":\"Describe your routine.\"}";
        String out = service.extractReferenceText(raw);
        assertEquals("Describe your routine.", out);
    }

    @Test
    void returnsCleanedRawTextWhenNotJson() {
        String raw = "```\nJust speak naturally about hobbies.\n```";
        String out = service.extractReferenceText(raw);
        assertFalse(out.contains("```"));
        assertEquals("Just speak naturally about hobbies.", out);
    }
}
