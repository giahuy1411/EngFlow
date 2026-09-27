package com.datn.engflow.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for audit-v19 W1.
 *
 * <p>Measured live 2026-09-27: {@code POST /api/ai/generate-vocab} intermittently returned a
 * Chinese gloss in the Vietnamese definition slot (鱼 "ocean life", 雨水 "weather",
 * 天气/气候 — reproduced via Playwright MCP). The local model ({@code qwen2.5:1.5b}) is
 * Chinese-centric and the prompt named no target language for the field. The fix is two-layer:
 * a stricter prompt AND this deterministic guard, because a 1.5B model cannot be trusted to obey
 * prompt wording. These tests pin the guard itself (no LLM call).
 */
class AiVocabServiceLanguageGuardTest {

    @Test
    @DisplayName("CJK (Han) characters are detected")
    void detectsCjk() {
        assertTrue(AiVocabService.containsCjk("鱼"), "simplified Han");
        assertTrue(AiVocabService.containsCjk("雨水"), "Han in a phrase");
        assertTrue(AiVocabService.containsCjk("天气 and 气候"), "Han mixed with Latin");
        assertTrue(AiVocabService.containsCjk("一种起源于美国"), "long Han string");
    }

    @Test
    @DisplayName("Vietnamese and English text are NOT flagged (no false positives)")
    void doesNotFlagLatin() {
        assertFalse(AiVocabService.containsCjk("cá sinh sống trong nước"), "Vietnamese with diacritics");
        assertFalse(AiVocabService.containsCjk("of or relating to the sea"), "English");
        assertFalse(AiVocabService.containsCjk("động vật biển khổng lồ"), "Vietnamese diacritics");
        assertFalse(AiVocabService.containsCjk(""), "empty");
        assertFalse(AiVocabService.containsCjk(null), "null");
    }

    @Test
    @DisplayName("Vietnamese diacritics are in Latin Extended Additional, NOT the Han block")
    void vietnameseDiacriticsAreLatin() {
        // U+1EA1 (ạ), U+1EC7 (ệ), U+1EDD (ờ) live in Latin Extended Additional, not CJK.
        assertFalse(AiVocabService.containsCjk("học tập nghiêm túc"));
        assertFalse(AiVocabService.containsCjk("đường phố"));
    }

    @Test
    @DisplayName("the lenient parser still works alongside the guard")
    void lenientParseUnchanged() throws Exception {
        List<Map<String, String>> out = AiVocabService.readVocabListLenient(
                "[{\"word\":\"sea\",\"definitionVi\":\"biển\",\"definitionEn\":\"the sea\"}]");
        assertFalse(AiVocabService.containsCjk(out.get(0).get("definitionVi")));
    }
}
