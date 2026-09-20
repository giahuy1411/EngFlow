package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.SpeakingSubmission;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.SpeakingPromptRepository;
import com.datn.engflow.repository.SpeakingSubmissionRepository;
import com.datn.engflow.service.assessment.SpeakingAssessmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakingSubmissionServiceAuthorizationTest {

    @Mock
    private SpeakingSubmissionRepository submissionRepository;
    @Mock
    private SpeakingPromptRepository promptRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MinioService minioService;
    @Mock
    private SpeakingAssessmentService assessmentService;

    private SpeakingSubmissionService service;
    private SpeakingSubmission submission;

    @BeforeEach
    void setUp() {
        service = new SpeakingSubmissionService(
                submissionRepository,
                new com.datn.engflow.security.MediaSigner("test-secret-0123456789abcdef"),
                promptRepository,
                userRepository,
                minioService,
                assessmentService,
                new ObjectMapper(), org.mockito.Mockito.mock(StudyActivityService.class)
        );
        submission = SpeakingSubmission.builder()
                .id(10L)
                .user(User.builder().id(5L).build())
                .build();
        when(submissionRepository.findById(10L)).thenReturn(Optional.of(submission));
    }

    @Test
    void ownerCanReadSubmission() {
        SpeakingSubmission result = service.getSubmissionForViewer(10L, 5L, false);

        assertThat(result).isSameAs(submission);
    }

    @Test
    void adminCanReadAnySubmission() {
        SpeakingSubmission result = service.getSubmissionForViewer(10L, 99L, true);

        assertThat(result).isSameAs(submission);
    }

    @Test
    void anotherUserCannotReadSubmission() {
        assertThatThrownBy(() -> service.getSubmissionForViewer(10L, 6L, false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Bạn không có quyền xem bài nộp này");
    }
}
