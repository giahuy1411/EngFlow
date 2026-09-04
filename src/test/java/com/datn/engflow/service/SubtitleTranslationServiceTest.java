package com.datn.engflow.service;

import com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine;
import com.datn.engflow.service.assessment.LlmChatClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtitleTranslationServiceTest {

    @Mock private LlmChatClient llm;

    private SubtitleTranslationService service;

    @BeforeEach
    void setUp() {
        service = new SubtitleTranslationService(llm, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void fillsVietnameseOnlyForMissingLines() {
        when(llm.completeWithRetry(anyString(), anyString())).thenReturn(
                "[\"Chào bạn\",\"Mình ổn, cảm ơn.\"]");
        List<TranscriptLine> lines = List.of(
                new TranscriptLine(1, 2, "Hello", null),
                new TranscriptLine(2, 3, "I'm fine thanks.", "Cảm ơn mình ổn"));

        List<TranscriptLine> result = service.translate(lines);

        assertThat(result.get(0).textVi()).isEqualTo("Chào bạn");
        // Existing translation is preserved; LLM output for it is never requested.
        assertThat(result.get(1).textVi()).isEqualTo("Cảm ơn mình ổn");
    }

    @Test
    void returnsOriginalLinesWhenLlmFails() {
        when(llm.completeWithRetry(anyString(), anyString()))
                .thenThrow(new IllegalStateException("LLM không phản hồi"));
        List<TranscriptLine> lines = List.of(new TranscriptLine(1, 2, "Hello", null));

        List<TranscriptLine> result = service.translate(lines);

        assertThat(result.get(0).textVi()).isNull();
        assertThat(result.get(0).textEn()).isEqualTo("Hello");
    }

    @Test
    void stripsMarkdownFencesAroundJsonArray() {
        when(llm.completeWithRetry(anyString(), anyString())).thenReturn(
                "```json\n[\"Chào bạn\"]\n```");
        List<TranscriptLine> lines = List.of(new TranscriptLine(1, 2, "Hello", null));

        List<TranscriptLine> result = service.translate(lines);

        assertThat(result.get(0).textVi()).isEqualTo("Chào bạn");
    }

    @Test
    void emptyInputPassesThrough() {
        assertThat(service.translate(null)).isNull();
        assertThat(service.translate(List.of())).isEmpty();
    }
}
