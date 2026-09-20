package com.datn.engflow.service;

import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.UserVocabularyProgress;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.UserVocabularyProgressRepository;
import com.datn.engflow.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditV9SrsIntervalCapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private UserVocabularyProgressRepository progressRepository;

    @Mock
    private DeckWordRepository deckWordRepository;

    private SrsService srsService;

    private UserVocabularyProgress progress;

    @BeforeEach
    void setUp() {
        srsService = new SrsService(progressRepository, userRepository, vocabularyRepository, deckWordRepository,
                org.mockito.Mockito.mock(StudyActivityService.class));

        User user = User.builder().id(2L).build();
        Vocabulary vocabulary = Vocabulary.builder().id(10017L).build();
        progress = UserVocabularyProgress.builder()
                .user(user)
                .vocabulary(vocabulary)
                .repetitions(15)
                .interval(1_537_216)
                .easeFactor(2.6)
                .reviewCount(42)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(vocabularyRepository.findById(10017L)).thenReturn(Optional.of(vocabulary));
        when(progressRepository.findByUserIdAndVocabularyId(2L, 10017L)).thenReturn(Optional.of(progress));
        when(progressRepository.save(any(UserVocabularyProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void reviewWord_capsHugeIntervalInsideDatetime2Range() {
        LocalDateTime before = LocalDateTime.now();

        srsService.reviewWord(2L, 10017L, 5);

        ArgumentCaptor<UserVocabularyProgress> captor = ArgumentCaptor.forClass(UserVocabularyProgress.class);
        verify(progressRepository).save(captor.capture());
        UserVocabularyProgress saved = captor.getValue();

        assertThat(progress.getInterval()).isEqualTo(365);
        assertThat(progress.getNextReviewDate())
                .isAfterOrEqualTo(before.plusDays(365))
                .isBeforeOrEqualTo(LocalDateTime.now().plusDays(365).plusSeconds(1));
        assertThat(saved.getInterval()).isEqualTo(365);
    }

    @Test
    void reviewWord_repairsNegativeIntervalToOneDay() {
        progress.setInterval(-1);
        progress.setRepetitions(15);
        LocalDateTime before = LocalDateTime.now();

        srsService.reviewWord(2L, 10017L, 5);

        ArgumentCaptor<UserVocabularyProgress> captor = ArgumentCaptor.forClass(UserVocabularyProgress.class);
        verify(progressRepository).save(captor.capture());

        assertThat(progress.getInterval()).isEqualTo(1);
        assertThat(progress.getRepetitions()).isEqualTo(16);
        assertThat(progress.getNextReviewDate())
                .isAfterOrEqualTo(before.plusDays(1))
                .isBeforeOrEqualTo(LocalDateTime.now().plusDays(1));
    }

    @ParameterizedTest
    @CsvSource({"364, 364", "365, 365", "366, 365"})
    void reviewWord_respectsUpperBoundary(int storedInterval, int expectedInterval) {
        progress.setInterval(storedInterval);
        progress.setEaseFactor(1.0);

        srsService.reviewWord(2L, 10017L, 3);

        verify(progressRepository).save(progress);
        assertThat(progress.getInterval()).isEqualTo(expectedInterval);
    }

    @Test
    void failedReview_preservesResetBehaviorForPreviouslyLargeInterval() {
        srsService.reviewWord(2L, 10017L, 2);

        verify(progressRepository).save(progress);
        assertThat(progress.getInterval()).isEqualTo(1);
        assertThat(progress.getRepetitions()).isZero();
    }
}
