package com.datn.engflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptServiceFullPromptTest {

    private AiPromptService service;

    @BeforeEach
    void setUp() {
        service = new AiPromptService("test-key", "http://localhost:0", "qwen2.5:1.5b", new ObjectMapper());
    }

    @Test
    void parsesJsonOutputIntoFormFields() {
        String raw = "{\"title\":\"Ordering Coffee\",\"description\":\"Luyện hội thoại gọi cà phê.\","
                + "\"prompt\":\"Đọc theo mẫu câu sau.\",\"referenceText\":\"Hi, can I get a latte?\","
                + "\"category\":\"daily\"}";

        Map<String, String> result = service.parseFullPrompt(raw, "coffee", "A2");

        assertThat(result.get("title")).isEqualTo("Ordering Coffee");
        assertThat(result.get("description")).contains("cà phê");
        assertThat(result.get("referenceText")).isEqualTo("Hi, can I get a latte?");
        assertThat(result.get("topic")).isEqualTo("coffee");
    }

    @Test
    void stripsMarkdownFencesBeforeParsing() {
        String raw = "```json\n{\"title\":\"T1\",\"referenceText\":\"Hello there.\"}\n```";

        Map<String, String> result = service.parseFullPrompt(raw, "topic", "B1");

        assertThat(result.get("title")).isEqualTo("T1");
        assertThat(result.get("referenceText")).isEqualTo("Hello there.");
    }

    @Test
    void nonJsonOutputFallsBackToRawReferenceText() {
        Map<String, String> result = service.parseFullPrompt("Just read this sentence aloud.", "topic", "A2");

        assertThat(result.get("referenceText")).isEqualTo("Just read this sentence aloud.");
        assertThat(result.get("topic")).isEqualTo("topic");
        assertThat(result.get("level")).isEqualTo("A2");
    }

    @Test
    void blankInputStillReturnsTopicAndLevel() {
        Map<String, String> result = service.parseFullPrompt(null, "my topic", null);

        assertThat(result.get("topic")).isEqualTo("my topic");
        assertThat(result.get("level")).isEqualTo("A2");
        assertThat(result).doesNotContainKey("title");
    }
}
