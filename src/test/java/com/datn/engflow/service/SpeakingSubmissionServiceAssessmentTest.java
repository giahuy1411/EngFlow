package com.datn.engflow.service;

import com.datn.engflow.model.entity.SpeakingSubmissionStatus;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.SpeakingPrompt;
import com.datn.engflow.model.entity.SpeakingSubmission;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.SpeakingPromptRepository;
import com.datn.engflow.repository.SpeakingSubmissionRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakingSubmissionServiceAssessmentTest {

    @Mock private SpeakingSubmissionRepository submissionRepository;
    @Mock private SpeakingPromptRepository promptRepository;
    @Mock private UserRepository userRepository;
    @Mock private MinioService minioService;

    private SpeakingSubmissionService service;
    private MockMultipartFile media;
    private User student;

    @BeforeEach
    void setUp() throws Exception {
        service = new SpeakingSubmissionService(
                submissionRepository, promptRepository, userRepository, minioService);
        media = new MockMultipartFile("media", "recording.webm", "audio/webm", new byte[]{1, 2, 3});
        student = User.builder().id(7L).build();
        when(promptRepository.findById(11L)).thenReturn(Optional.of(
                SpeakingPrompt.builder().id(11L).prompt("Describe your day").build()));
        when(minioService.uploadMedia(anyString(), eq(media))).thenReturn("speaking/object.webm");
        when(submissionRepository.save(any(SpeakingSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void uploadQueuesSubmissionForManualReviewWithoutAiAssessment() {
        SpeakingSubmission result = service.uploadSubmission(media, 11L, student);

        assertThat(result.getStatus()).isEqualTo(SpeakingSubmissionStatus.SUBMITTED);
        assertThat(result.getMediaObjectKey()).isEqualTo("speaking/object.webm");
        assertThat(result.getMediaType()).isEqualTo("audio/webm");
        assertThat(result.getScore()).isNull();
    }
}