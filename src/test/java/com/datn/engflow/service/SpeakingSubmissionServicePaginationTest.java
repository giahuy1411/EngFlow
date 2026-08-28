package com.datn.engflow.service;

import com.datn.engflow.model.entity.SpeakingSubmissionStatus;
import com.datn.engflow.model.entity.SpeakingSubmission;
import com.datn.engflow.repository.SpeakingSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakingSubmissionServicePaginationTest {

    @Mock
    private SpeakingSubmissionRepository submissionRepository;

    @InjectMocks
    private SpeakingSubmissionService service;

    @Test
    void userSubmissionsArePaginated() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SpeakingSubmission> expected = Page.empty(pageable);
        when(submissionRepository.findByUserId(7L, pageable)).thenReturn(expected);

        assertThat(service.getUserSubmissions(7L, pageable)).isSameAs(expected);
        verify(submissionRepository).findByUserId(7L, pageable);
    }

    @Test
    void adminSubmissionsArePaginated() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SpeakingSubmission> expected = Page.empty(pageable);
        when(submissionRepository.findAll(pageable)).thenReturn(expected);

        assertThat(service.getAllSubmissionsForAdmin(null, pageable)).isSameAs(expected);
        verify(submissionRepository).findAll(pageable);
    }

    @Test
    void adminSubmissionsCanBeFilteredByStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SpeakingSubmission> expected = Page.empty(pageable);
        when(submissionRepository.findByStatus(SpeakingSubmissionStatus.SUBMITTED, pageable)).thenReturn(expected);

        assertThat(service.getAllSubmissionsForAdmin(SpeakingSubmissionStatus.SUBMITTED, pageable))
                .isSameAs(expected);
        verify(submissionRepository).findByStatus(SpeakingSubmissionStatus.SUBMITTED, pageable);
    }

    @Test
    void promptSubmissionsAreScopedToTheCurrentUser() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SpeakingSubmission> expected = Page.empty(pageable);
        when(submissionRepository.findByUserIdAndPromptId(7L, 3L, pageable)).thenReturn(expected);

        assertThat(service.getUserPromptSubmissions(7L, 3L, pageable)).isSameAs(expected);
        verify(submissionRepository).findByUserIdAndPromptId(7L, 3L, pageable);
    }
}
