package com.datn.engflow.service.assessment;

import com.datn.engflow.service.MinioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SpeakingTranscriptClientTest {

    @Mock private MinioService minioService;

    private SpeakingAssessmentService newService(String whisperBaseUrl) {
        return new SpeakingAssessmentService(
                minioService, whisperBaseUrl,
                "http://localhost:1/v1", "unused-model", 5L, new ObjectMapper());
    }

    @Test
    void unconfiguredSidecarIsReportedAsNotConfigured() {
        SpeakingAssessmentService.SpeakingTranscriptClient client =
                new SpeakingAssessmentService.SpeakingTranscriptClient(null, Duration.ofSeconds(5));

        assertThat(client.isConfigured()).isFalse();
    }

    @Test
    void blankBaseUrlAlsoDisablesSidecar() {
        SpeakingAssessmentService.SpeakingTranscriptClient client =
                new SpeakingAssessmentService.SpeakingTranscriptClient("   ", Duration.ofSeconds(5));

        assertThat(client.isConfigured()).isFalse();
    }

    @Test
    void configuredBaseUrlEnablesSidecarAndStripsTrailingSlash() {
        SpeakingAssessmentService.SpeakingTranscriptClient client =
                new SpeakingAssessmentService.SpeakingTranscriptClient(
                        "http://whisper:9002///", Duration.ofSeconds(5));

        assertThat(client.isConfigured()).isTrue();
    }

    @Test
    void transcribeReturnsNullForEmptyAudioWithoutCallingNetwork() {
        SpeakingAssessmentService.SpeakingTranscriptClient client =
                new SpeakingAssessmentService.SpeakingTranscriptClient(
                        "http://127.0.0.1:1", Duration.ofSeconds(2));

        assertThat(client.transcribe(new byte[0], "audio/webm")).isNull();
        assertThat(client.transcribe(null, "audio/webm")).isNull();
    }

    @Test
    void transcribeReturnsNullWhenSidecarUnreachable() {
        // Port 1 on loopback refuses instantly; must degrade to null, never throw.
        SpeakingAssessmentService.SpeakingTranscriptClient client =
                new SpeakingAssessmentService.SpeakingTranscriptClient(
                        "http://127.0.0.1:1", Duration.ofSeconds(2));

        assertThat(client.transcribe(new byte[]{1, 2, 3}, "audio/webm")).isNull();
    }

    @Test
    void assessFailsGracefullyWhenNoTranscriptAndNoUsableAudio() {
        SpeakingAssessmentService service = newService("http://127.0.0.1:1");
        com.datn.engflow.model.entity.SpeakingSubmission submission =
                com.datn.engflow.model.entity.SpeakingSubmission.builder()
                        .id(1L)
                        .mediaObjectKey("speaking/missing.webm")
                        .mediaType("audio/webm")
                        .build();
        org.mockito.Mockito.when(minioService.getObject("speaking/missing.webm")).thenReturn(null);

        SpeakingAssessmentOutcome outcome = service.assess(submission);

        assertThat(outcome.transcript()).isNull();
        assertThat(outcome.transcriptSource()).isEqualTo("NONE");
        assertThat(outcome.rubric()).isNull();
        assertThat(outcome.error()).contains("transcript");
        assertThat(outcome.provider()).isEqualTo(SpeakingAssessmentService.PROVIDER);
    }

    @Test
    void assessUsesUserTranscriptAndKeepsAlignmentWhenLlmFails() {
        // Ollama URL points at a dead port: rubric must fail, but transcript+alignment survive.
        SpeakingAssessmentService service = newService("");
        com.datn.engflow.model.entity.SpeakingSubmission submission =
                com.datn.engflow.model.entity.SpeakingSubmission.builder()
                        .id(2L)
                        .transcript("i wake up at seven")
                        .prompt(com.datn.engflow.model.entity.SpeakingPrompt.builder()
                                .referenceText("I wake up at seven o'clock")
                                .build())
                        .build();

        SpeakingAssessmentOutcome outcome = service.assess(submission);

        assertThat(outcome.transcript()).isEqualTo("i wake up at seven");
        assertThat(outcome.transcriptSource()).isEqualTo("USER");
        assertThat(outcome.alignment()).isNotNull();
        assertThat(outcome.alignment().coveragePercent()).isGreaterThan(0.0);
        assertThat(outcome.rubric()).isNull();
        assertThat(outcome.error()).contains("rubric");
    }
}
