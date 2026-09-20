package com.datn.engflow.service;

import com.datn.engflow.model.entity.SpeakingSubmissionStatus;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.SpeakingPrompt;
import com.datn.engflow.model.entity.SpeakingSubmission;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.SpeakingPromptRepository;
import com.datn.engflow.repository.SpeakingSubmissionRepository;
import com.datn.engflow.service.assessment.SpeakingAssessmentOutcome;
import com.datn.engflow.service.assessment.SpeakingAssessmentService;
import com.datn.engflow.service.assessment.SpeakingRubricResult;
import com.datn.engflow.service.assessment.TranscriptAlignmentMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakingSubmissionServiceAssessmentTest {

    @Mock private SpeakingSubmissionRepository submissionRepository;
    @Mock private SpeakingPromptRepository promptRepository;
    @Mock private UserRepository userRepository;
    @Mock private MinioService minioService;
    @Mock private SpeakingAssessmentService assessmentService;
    @Mock private StudyActivityService studyActivityService;

    private SpeakingSubmissionService service;
    private MockMultipartFile media;
    private User student;

    @BeforeEach
    void setUp() throws Exception {
        service = new SpeakingSubmissionService(
                submissionRepository, new com.datn.engflow.security.MediaSigner("test-secret-0123456789abcdef"), promptRepository, userRepository, minioService,
                assessmentService, new ObjectMapper(), studyActivityService);
        media = new MockMultipartFile("media", "recording.webm", "audio/webm", new byte[]{1, 2, 3});
        student = User.builder().id(7L).build();
        lenient().when(promptRepository.findById(11L)).thenReturn(Optional.of(
                SpeakingPrompt.builder().id(11L).prompt("Describe your day").build()));
        lenient().when(minioService.uploadMedia(anyString(), eq(media))).thenReturn("speaking/object.webm");
        lenient().when(submissionRepository.save(any(SpeakingSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void uploadQueuesSubmissionForManualReviewWithoutAiAssessment() {
        SpeakingSubmission result = service.uploadSubmission(media, 11L, student);

        assertThat(result.getStatus()).isEqualTo(SpeakingSubmissionStatus.SUBMITTED);
        assertThat(result.getMediaObjectKey()).isEqualTo("speaking/object.webm");
        assertThat(result.getMediaType()).isEqualTo("audio/webm");
        assertThat(result.getScore()).isNull();
        org.mockito.Mockito.verifyNoInteractions(studyActivityService);
    }

    @Test
    void assessSubmissionStoresRubricAndAlignmentOnSuccess() {
        SpeakingSubmission submission = SpeakingSubmission.builder()
                .id(5L)
                .user(student)
                .status(SpeakingSubmissionStatus.SUBMITTED)
                .prompt(SpeakingPrompt.builder().id(11L).referenceText("Hello world").build())
                .build();
        when(submissionRepository.findById(5L)).thenReturn(Optional.of(submission));
        TranscriptAlignmentMetrics.AlignmentResult alignment =
                TranscriptAlignmentMetrics.compute("Hello world", "hello world");
        SpeakingAssessmentOutcome outcome = new SpeakingAssessmentOutcome(
                "hello world", "WHISPER", alignment,
                new SpeakingRubricResult(7, 6, 8, "Phát âm tốt"), "LOCAL_WHISPER_LLM", null);
        when(assessmentService.assess(submission)).thenReturn(outcome);

        SpeakingSubmission result = service.assessSubmission(5L);

        assertThat(result.getStatus()).isEqualTo(SpeakingSubmissionStatus.COMPLETED);
        assertThat(result.getTranscript()).isEqualTo("hello world");
        assertThat(result.getScoreGrammar()).isEqualTo(7);
        assertThat(result.getScoreVocabulary()).isEqualTo(6);
        assertThat(result.getScoreFluency()).isEqualTo(8);
        assertThat(result.getScoreTotal()).isEqualTo(7.0);
        assertThat(result.getFeedback()).isEqualTo("Phát âm tốt");
        assertThat(result.getPronunciationCompleteness()).isEqualTo(100.0);
        assertThat(result.getPronunciationDetailsJson()).contains("wordErrorRate");
        assertThat(result.getAssessmentError()).isNull();
        org.mockito.Mockito.verify(studyActivityService).recordStudy(student.getId());
    }

    @Test
    void assessSubmissionMarksFailedWhenTranscriptUnavailable() {
        SpeakingSubmission submission = SpeakingSubmission.builder()
                .id(6L)
                .status(SpeakingSubmissionStatus.SUBMITTED)
                .prompt(SpeakingPrompt.builder().id(11L).build())
                .build();
        when(submissionRepository.findById(6L)).thenReturn(Optional.of(submission));
        when(assessmentService.assess(submission)).thenReturn(
                SpeakingAssessmentOutcome.failed("LOCAL_WHISPER_LLM", "Chưa có transcript"));

        SpeakingSubmission result = service.assessSubmission(6L);

        assertThat(result.getStatus()).isEqualTo(SpeakingSubmissionStatus.FAILED);
        assertThat(result.getAssessmentError()).contains("transcript");
        assertThat(result.getScoreTotal()).isNull();
        org.mockito.Mockito.verifyNoInteractions(studyActivityService);
    }

    @Test
    void completedSubmissionCannotEarnAnotherDayByReassessment() {
        SpeakingSubmission submission = SpeakingSubmission.builder().id(9L).user(student)
                .status(SpeakingSubmissionStatus.COMPLETED).transcript("hello").build();
        when(submissionRepository.findById(9L)).thenReturn(Optional.of(submission));
        assertThat(service.assessSubmission(9L)).isSameAs(submission);
        org.mockito.Mockito.verifyNoInteractions(assessmentService, studyActivityService);
    }

    @Test
    void emptyTranscriptDoesNotCountEvenWhenRubricExists() {
        SpeakingSubmission submission = SpeakingSubmission.builder().id(12L).user(student)
                .status(SpeakingSubmissionStatus.SUBMITTED).build();
        when(submissionRepository.findById(12L)).thenReturn(Optional.of(submission));
        when(assessmentService.assess(submission)).thenReturn(new SpeakingAssessmentOutcome(
                "  ", "WHISPER", null, new SpeakingRubricResult(0, 0, 0, "Không nghe rõ"),
                "LOCAL_WHISPER_LLM", null));

        assertThat(service.assessSubmission(12L).getStatus()).isEqualTo(SpeakingSubmissionStatus.FAILED);
        org.mockito.Mockito.verifyNoInteractions(studyActivityService);
    }
}
