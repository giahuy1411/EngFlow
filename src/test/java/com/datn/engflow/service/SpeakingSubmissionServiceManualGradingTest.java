package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.model.dto.request.GradeSpeakingSubmissionRequest;
import com.datn.engflow.model.entity.SpeakingSubmissionStatus;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.SpeakingSubmission;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.SpeakingPromptRepository;
import com.datn.engflow.repository.SpeakingSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakingSubmissionServiceManualGradingTest {

    @Mock private SpeakingSubmissionRepository submissionRepository;
    @Mock private SpeakingPromptRepository promptRepository;
    @Mock private UserRepository userRepository;
    @Mock private MinioService minioService;

    private SpeakingSubmissionService service;
    private SpeakingSubmission submission;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new SpeakingSubmissionService(
                submissionRepository, promptRepository, userRepository, minioService);
        submission = SpeakingSubmission.builder()
                .id(20L)
                .status(SpeakingSubmissionStatus.SUBMITTED)
                .build();
        admin = User.builder().id(2L).isAdmin(true).build();
    }

    @Test
    void adminGradesSubmissionOnTenPointScale() {
        when(submissionRepository.findById(20L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(submissionRepository.save(any(SpeakingSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SpeakingSubmission result = service.gradeSubmission(
                20L, 2L, new GradeSpeakingSubmissionRequest(new BigDecimal("8.5"), "Phát âm rõ, cần nói tự nhiên hơn.", "Kiểm tra lại lần sau"));

        assertThat(result.getStatus()).isEqualTo(SpeakingSubmissionStatus.GRADED);
        assertThat(result.getScore()).isEqualByComparingTo("8.5");
        assertThat(result.getAdminFeedback()).isEqualTo("Phát âm rõ, cần nói tự nhiên hơn.");
        assertThat(result.getPrivateNote()).isEqualTo("Kiểm tra lại lần sau");
        assertThat(result.getGradedBy()).isSameAs(admin);
        assertThat(result.getGradedAt()).isNotNull();
        verify(submissionRepository).save(submission);
    }

    @Test
    void gradeRejectsScoreAboveTen() {
        assertThatThrownBy(() -> service.gradeSubmission(
                20L, 2L, new GradeSpeakingSubmissionRequest(new BigDecimal("10.5"), "Nhận xét", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Điểm phải nằm trong khoảng từ 0 đến 10");
    }

    @Test
    void gradeRequiresFeedback() {
        assertThatThrownBy(() -> service.gradeSubmission(
                20L, 2L, new GradeSpeakingSubmissionRequest(new BigDecimal("8.0"), "  ", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Nhận xét không được để trống");
    }
}
