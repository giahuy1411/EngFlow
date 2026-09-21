package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.FlashcardReviewRequest;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.UserVocabularyProgress;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.UserVocabularyProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * audit-v12 F148: the flashcard review path must delegate to {@link SrsService} so there is
 * ONE spaced-repetition algorithm. Previously this service had its own 1/3/7/14-day table
 * and never wrote {@code ease_factor} / {@code repetitions}, while {@code SrsService}
 * wrote the same rows with SM-2 — the two drifted apart (11/14 live rows stuck at
 * {@code ease_factor = 2.5}).
 *
 * <p>The streak contract is unchanged and still asserted here: one flashcard review is one
 * real study activity. It is now satisfied transitively, because
 * {@code SrsService.reviewWord} is what calls {@code recordStudy}.
 */
@ExtendWith(MockitoExtension.class)
class FlashcardServiceStudyActivityTest {
    @Mock private UserRepository userRepository;
    @Mock private UserVocabularyProgressRepository progressRepository;
    @Mock private SrsService srsService;
    @InjectMocks private FlashcardService service;

    private void existingUser() {
        when(userRepository.findByEmail("study@example.test"))
                .thenReturn(Optional.of(User.builder().id(42L).build()));
    }

    private FlashcardReviewRequest review(int quality) {
        FlashcardReviewRequest request = new FlashcardReviewRequest();
        request.setVocabularyId(9L);
        request.setQuality(quality);
        return request;
    }

    @Test
    void reviewDelegatesToSrsWithTheGivenQuality() {
        existingUser();

        service.reviewFlashcard(review(5), "study@example.test");

        // The quality the UI sent must reach SM-2 unchanged — that is what makes "Dễ"
        // different from "Tiếp theo".
        verify(srsService).reviewWord(42L, 9L, 5);
    }

    /** "Lại" (quality 1) is still a real practice attempt and must not be skipped. */
    @Test
    void lowQualityStillRecorded() {
        existingUser();

        service.reviewFlashcard(review(1), "study@example.test");

        verify(srsService).reviewWord(42L, 9L, 1);
    }

    @Test
    void srsFailurePropagatesSoTheTransactionRollsBack() {
        existingUser();
        doThrow(new IllegalStateException("srs failed")).when(srsService).reviewWord(anyLong(), anyLong(), anyInt());

        assertThatThrownBy(() -> service.reviewFlashcard(review(4), "study@example.test"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getStatusReturnsMasteryLevelOrDefaultZero() {
        existingUser();
        when(progressRepository.findByUserIdAndVocabularyId(42L, 9L))
                .thenReturn(Optional.of(UserVocabularyProgress.builder().masteryLevel(2).build()));

        assertThat(service.getStatus(9L, "study@example.test")).isEqualTo(2);

        when(progressRepository.findByUserIdAndVocabularyId(42L, 8L)).thenReturn(Optional.empty());
        assertThat(service.getStatus(8L, "study@example.test")).isZero();
    }
}
