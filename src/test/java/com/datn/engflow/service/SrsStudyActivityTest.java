package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SrsStudyActivityTest {
    @Mock private UserVocabularyProgressRepository progressRepository;
    @Mock private UserRepository userRepository;
    @Mock private VocabularyRepository vocabularyRepository;
    @Mock private DeckWordRepository deckWordRepository;
    @Mock private StudyActivityService studyActivityService;
    @InjectMocks private SrsService service;

    private void existingWord() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(User.builder().id(42L).build()));
        when(vocabularyRepository.findById(9L)).thenReturn(Optional.of(Vocabulary.builder().id(9L).build()));
    }

    @Test
    void incorrectReviewCountsAfterSavingProgress() {
        existingWord();
        service.reviewWord(42L, 9L, 0);
        var order = inOrder(progressRepository, studyActivityService);
        order.verify(progressRepository).save(any());
        order.verify(studyActivityService).recordStudy(42L);
    }

    @Test
    void persistenceFailureDoesNotCount() {
        existingWord();
        when(progressRepository.save(any())).thenThrow(new IllegalStateException("save failed"));
        assertThatThrownBy(() -> service.reviewWord(42L, 9L, 5))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(studyActivityService);
    }

    @Test
    void invalidRatingDoesNotWrite() {
        assertThatThrownBy(() -> service.reviewWord(42L, 9L, 6))
                .isInstanceOf(com.datn.engflow.exception.BadRequestException.class);
        verifyNoInteractions(studyActivityService, progressRepository, userRepository, vocabularyRepository);
    }
}
