package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.FlashcardReviewRequest;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.UserVocabularyProgressRepository;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Flashcard review là hoạt động học thật, nên phải tính ngày học giống SRS review.
 * Trước đây luồng này không gọi {@code recordStudy}, khiến user học flashcard thấy
 * streak không tăng và rơi khỏi danh sách nhận mail nhắc — trong khi thao tác tương
 * đương ở {@code /api/srs/review} thì được tính.
 */
@ExtendWith(MockitoExtension.class)
class FlashcardServiceStudyActivityTest {
    @Mock private UserVocabularyProgressRepository progressRepository;
    @Mock private UserRepository userRepository;
    @Mock private VocabularyRepository vocabularyRepository;
    @Mock private StudyActivityService studyActivityService;
    @InjectMocks private FlashcardService service;

    private void existingWord() {
        when(userRepository.findByEmail("study@example.test"))
                .thenReturn(Optional.of(User.builder().id(42L).build()));
        when(vocabularyRepository.findById(9L))
                .thenReturn(Optional.of(Vocabulary.builder().id(9L).build()));
    }

    private FlashcardReviewRequest review(boolean isKnown) {
        FlashcardReviewRequest request = new FlashcardReviewRequest();
        request.setVocabularyId(9L);
        request.setIsKnown(isKnown);
        return request;
    }

    @Test
    void reviewCountsStudyDayAfterSavingProgress() {
        existingWord();

        service.reviewFlashcard(review(true), "study@example.test");

        var order = inOrder(progressRepository, studyActivityService);
        order.verify(progressRepository).save(any());
        order.verify(studyActivityService).recordStudy(42L);
    }

    /** "Chưa thuộc" vẫn là một lượt luyện tập thật — không được bỏ qua. */
    @Test
    void unknownRatingStillCounts() {
        existingWord();

        service.reviewFlashcard(review(false), "study@example.test");

        verify(studyActivityService).recordStudy(42L);
    }

    @Test
    void persistenceFailureDoesNotCount() {
        existingWord();
        when(progressRepository.save(any())).thenThrow(new IllegalStateException("save failed"));

        assertThatThrownBy(() -> service.reviewFlashcard(review(true), "study@example.test"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(studyActivityService);
    }
}
