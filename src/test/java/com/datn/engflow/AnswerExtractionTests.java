package com.datn.engflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

class AnswerExtractionTests {

    // Simulates the core logic from HtmlParserService.extractAnswerForQuestion
    private String extractAnswer(String answerContent, String questionNum) {
        if (answerContent == null || answerContent.isBlank()) return "";

        String normalized = answerContent
                .replaceAll("(?s)<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&[a-z]+;", " ")
                .replaceAll("\\u00a0", " ")
                .replaceAll("[\\s\\u00a0]+", " ").trim();

        // Strategy 1: Inline numbered format: "1 're  2 is  3 are"
        String sentinel = normalized + " 0 ";
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(
                "\\b" + questionNum + "\\s+([^\\d]+?)(?=\\s+\\d+\\s)");
        java.util.regex.Matcher m1 = p1.matcher(sentinel);
        if (m1.find()) {
            String ans = m1.group(1).trim();
            ans = ans.replaceAll("\"", "").trim();
            return ans.isEmpty() ? "" : ans;
        }

        // Strategy 2: "N. answer" or "N) answer" formats
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(
                questionNum + "[\\.\\)]\\s*([^\\d]+?)(?=\\s*\\d+[\\.\\)]\\s|\\s*$)");
        java.util.regex.Matcher m2 = p2.matcher(normalized);
        if (m2.find()) {
            return m2.group(1).trim().replaceAll("\"", "").trim();
        }

        return "";
    }

    @Test
    void testApostropheRe() {
        // The bug: was stripping apostrophe from 're, 'm, 've
        String result = extractAnswer("1 're  2 is  3 are", "1");
        assertEquals("'re", result, "Apostrophe in 're must be preserved");
    }

    @Test
    void testApostropheM() {
        String result = extractAnswer("1 'm  2 're  3 's", "1");
        assertEquals("'m", result, "Apostrophe in 'm must be preserved");
    }

    @Test
    void testApostropheVe() {
        String result = extractAnswer("1 've got  2 's got  3 haven't", "1");
        assertEquals("'ve got", result, "Apostrophe in 've must be preserved");
    }

    @Test
    void testMultiWordAnswer() {
        String result = extractAnswer("1 haven't got  2 hasn't got  3 have got", "2");
        assertEquals("hasn't got", result, "Multi-word answer with apostrophe");
    }

    @Test
    void testStandardDotFormat() {
        // "1. answer 2. next" format
        String result = extractAnswer("1. haven't got 2. hasn't got 3. have got", "1");
        assertEquals("haven't got", result);
    }

    @Test
    void testWithUnicodeApostrophe() {
        // Unicode curly apostrophes U+2019
        String content = "1 \u2019re  2 is  3 are";
        String result = extractAnswer(content, "1");
        assertEquals("\u2019re", result, "Unicode apostrophe preserved");
    }

    @Test
    void testLastAnswerNotCut() {
        // Bug: last answer was missing because sentinel didn't have trailing \d
        String result = extractAnswer("1 're  2 is  3 are", "3");
        assertEquals("are", result, "Last answer must be captured");
    }

    @Test
    void testDoubleQuoteStripped() {
        // Double quotes around answer should be stripped
        String result = extractAnswer("1 \"'re\"  2 \"is\"", "1");
        assertEquals("'re", result, "Only double quotes stripped");
    }

    @Test
    void testNegation() {
        String result = extractAnswer("1 isn't  2 aren't  3 'm not", "2");
        assertEquals("aren't", result);
    }
}
