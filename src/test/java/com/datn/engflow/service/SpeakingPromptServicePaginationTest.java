package com.datn.engflow.service;

import com.datn.engflow.model.entity.SpeakingPrompt;
import com.datn.engflow.repository.SpeakingPromptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakingPromptServicePaginationTest {

    @Mock
    private SpeakingPromptRepository repository;

    @InjectMocks
    private SpeakingPromptService service;

    @Test
    void publishedPromptsArePaginated() {
        Pageable pageable = PageRequest.of(0, 20);
        SpeakingPrompt prompt = SpeakingPrompt.builder().id(1L).build();
        Page<SpeakingPrompt> expected = new PageImpl<>(List.of(prompt), pageable, 1);
        when(repository.findByIsPublishedTrue(pageable)).thenReturn(expected);

        Page<SpeakingPrompt> result = service.getAllPrompts(pageable, true);

        assertThat(result.getContent()).containsExactly(prompt);
        verify(repository).findByIsPublishedTrue(pageable);
    }

    /** Viewer khong premium phai dung query loc isPremium = false. */
    @Test
    void freeViewersGetOnlyNonPremiumPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SpeakingPrompt> expected = Page.empty(pageable);
        when(repository.findByIsPremiumFalseAndIsPublishedTrue(pageable)).thenReturn(expected);

        assertThat(service.getAllPrompts(pageable, false)).isSameAs(expected);
        verify(repository).findByIsPremiumFalseAndIsPublishedTrue(pageable);
    }

    @Test
    void adminPromptsArePaginated() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<SpeakingPrompt> expected = Page.empty(pageable);
        when(repository.findAll(pageable)).thenReturn(expected);

        assertThat(service.getAllPromptsForAdmin(null, pageable)).isSameAs(expected);
        verify(repository).findAll(pageable);
    }

    @Test
    void keywordSearchUsesRepositoryQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SpeakingPrompt> expected = Page.empty(pageable);
        when(repository.searchByKeyword("travel", false, pageable)).thenReturn(expected);

        assertThat(service.getAllPrompts(" travel ", pageable, true)).isSameAs(expected);
        verify(repository).searchByKeyword("travel", false, pageable);
    }

    /** Voi viewer khong premium, search phai bat loc premium trong chinh query. */
    @Test
    void keywordSearchHidesPremiumForFreeViewers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SpeakingPrompt> expected = Page.empty(pageable);
        when(repository.searchByKeyword("travel", true, pageable)).thenReturn(expected);

        assertThat(service.getAllPrompts(" travel ", pageable, false)).isSameAs(expected);
        verify(repository).searchByKeyword("travel", true, pageable);
    }
}
