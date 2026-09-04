package com.datn.engflow.service;

import com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine;
import com.datn.engflow.model.entity.VideoAttempt;
import com.datn.engflow.model.entity.VideoLesson;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.VideoAttemptRepository;
import com.datn.engflow.service.assessment.LlmChatClient;
import com.datn.engflow.service.assessment.SpeakingAssessmentService;
import com.datn.engflow.service.assessment.TranscriptAlignmentMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShadowingAiGradingServiceTest {

    @Mock private VideoAttemptRepository attemptRepository;
    @Mock private VideoLessonService videoLessonService;
    @Mock private SpeakingAssessmentService assessmentService;
    @Mock private LlmChatClient llm;

    private ShadowingAiGradingService service;
    private VideoAttempt attempt;

    @BeforeEach
    void setUp() {
        service = new ShadowingAiGradingService(
                attemptRepository, videoLessonService, null, assessmentService, llm);
        attempt = VideoAttempt.builder()
                .id(9L)
                .lineIndex(0)
                .mediaObjectKey("video-attempts/audio.webm")
                .mediaType("audio/webm")
                .status("SUBMITTED")
                .videoLesson(VideoLesson.builder().id(3L).transcriptJson(
                        "[{\"start\":1,\"end\":4,\"textEn\":\"I would like a coffee please\"}]").build())
                .build();
        lenient().when(videoLessonService.readTranscript(anyString())).thenAnswer(invocation ->
                new ObjectMapper().readValue(
                        (String) invocation.getArgument(0),
                        new com.fasterxml.jackson.core.type.TypeReference<List<TranscriptLine>>() {
                        }));
        lenient().when(attemptRepository.findById(9L)).thenReturn(Optional.of(attempt));
        lenient().when(attemptRepository.save(any(VideoAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "minioService", null);
    }

    @Test
    void coverageScoreMapsPercentOntoTenScale() {
        assertThat(ShadowingAiGradingService.coverageScore(
                        TranscriptAlignmentMetrics.compute("I would like a coffee please", "I would like a coffee please")))
                .isEqualByComparingTo(new BigDecimal("10.0"));
        assertThat(ShadowingAiGradingService.coverageScore(
                        TranscriptAlignmentMetrics.compute("I would like a coffee please", "I would like a")))
                .isEqualByComparingTo(new BigDecimal("6.7"));
        assertThat(ShadowingAiGradingService.coverageScore(
                        TranscriptAlignmentMetrics.compute("I would like a coffee please", "")))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void aiGradeStoresScoreAndLlmFeedback() {
        when(assessmentService.transcribe("video-attempts/audio.webm", "audio/webm"))
                .thenReturn("I would like a coffee");
        when(llm.completeWithRetry(anyString(), anyString())).thenReturn("Bạn nói đúng hầu hết câu mẫu.");

        VideoAttempt result = service.aiGrade(9L);

        assertThat(result.getStatus()).isEqualTo("GRADED");
        assertThat(result.getScore()).isEqualByComparingTo(new BigDecimal("8.3"));
        assertThat(result.getAdminFeedback()).contains("Bạn nói đúng");
    }

    @Test
    void aiGradeFallsBackToCoverageFeedbackWhenLlmFails() {
        when(assessmentService.transcribe(anyString(), anyString())).thenReturn("I would like a coffee");
        when(llm.completeWithRetry(anyString(), anyString()))
                .thenThrow(new IllegalStateException("LLM không phản hồi"));

        VideoAttempt result = service.aiGrade(9L);

        assertThat(result.getStatus()).isEqualTo("GRADED");
        assertThat(result.getScore()).isNotNull();
        assertThat(result.getAdminFeedback()).isNotBlank();
    }

    @Test
    void aiGradeFallsBackWhenLlmRepliesInEnglish() {
        when(assessmentService.transcribe(anyString(), anyString())).thenReturn("I would like a coffee");
        // Small models sometimes ignore the Vietnamese instruction and answer in English.
        when(llm.completeWithRetry(anyString(), anyString()))
                .thenReturn("The student said most words correctly.");

        VideoAttempt result = service.aiGrade(9L);

        assertThat(result.getStatus()).isEqualTo("GRADED");
        assertThat(result.getScore()).isNotNull();
        assertThat(result.getAdminFeedback()).contains("Nghe lại");
        assertThat(result.getAdminFeedback()).doesNotContain("The student");
    }

    @Test
    void aiGradeZeroScoreWhenTranscriptEmpty() {
        when(assessmentService.transcribe(anyString(), anyString())).thenReturn("");

        VideoAttempt result = service.aiGrade(9L);

        assertThat(result.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getAdminFeedback()).contains("không nghe rõ");
        verify(llm, never()).completeWithRetry(anyString(), anyString());
    }

    @Test
    void aiGradeRefusesToOverwriteManualGrade() {
        attempt.setStatus("GRADED");
        attempt.setGradedBy(new User());
        assertThatThrownBy(() -> service.aiGrade(9L))
                .isInstanceOf(com.datn.engflow.exception.BadRequestException.class);
    }

    @Test
    void fallbackFeedbackEscalatesWithCoverage() {
        assertThat(ShadowingAiGradingService.coverageFallbackFeedback(
                        TranscriptAlignmentMetrics.compute("a b c d e f g h i j", "a b c d e f g h i j")))
                .contains("đúng");
        assertThat(ShadowingAiGradingService.coverageFallbackFeedback(
                        TranscriptAlignmentMetrics.compute("a b c d e f g h i j", "a b c d e")))
                .contains("Nghe chậm");
        assertThat(ShadowingAiGradingService.coverageFallbackFeedback(
                        TranscriptAlignmentMetrics.compute("a b c d e f g h i j", "")))
                .contains("Nghe chậm");
    }
}
