package com.datn.engflow.service;

import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.request.FlashcardReviewRequest;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.entity.UserVocabularyProgress;
import com.datn.engflow.repository.UserRepository;
import com.datn.engflow.repository.UserVocabularyProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * class FlashcardService.
 *
 * <p>audit-v12 F148: this service used to implement its OWN spaced-repetition schedule
 * (a fixed 1/3/7/14-day table driven by a binary known/unknown flag) and write it into the
 * same {@code user_vocabulary_progress} rows that {@link SrsService} owns with the SM-2
 * algorithm. The two schedules disagreed and neither knew about the other: a word reviewed
 * through the flashcard UI got {@code next_review_date} from the 1/3/7/14 table while
 * {@code ease_factor} / {@code repetitions} stayed at their defaults forever — measured in
 * the live DB as 11 of 14 rows stuck at {@code ease_factor = 2.5} and 2 rows with
 * {@code review_count > 2} but {@code repetitions = 0}.
 *
 * <p>There is now a single algorithm. {@link SrsService#reviewWord} is the only writer of
 * review state, so the flashcard UI and {@code POST /api/srs/review} cannot drift apart.
 */
public class FlashcardService {

    private final UserRepository userRepository;
    private final UserVocabularyProgressRepository progressRepository;
    private final SrsService srsService;
    private final StudyActivityService studyActivityService;

    @Transactional
    public void reviewFlashcard(FlashcardReviewRequest request, String email) {
        log.info("Review flashcard: vocabularyId={}, quality={}, email={}",
                request.getVocabularyId(), request.getQuality(), email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Single source of truth for spaced repetition. SrsService.reviewWord also records
        // the study day, so a flashcard review still counts toward the streak exactly as
        // before (locked by FlashcardServiceStudyActivityTest).
        srsService.reviewWord(user.getId(), request.getVocabularyId(), request.getQuality());
    }

    /**
     * audit-v12 F153: record that the learner studied today, for the streak.
     *
     * <p>The flashcard drill is now a plain back/continue reader — it sends no SM-2 quality,
     * so it no longer goes through {@link SrsService#reviewWord} and would otherwise stop
     * counting toward the streak. The games record their day on submit
     * ({@code GameService.checkin}); flashcard has no score to submit, so it records on the
     * first "continue" instead.
     *
     * <p>{@code recordStudy} is {@code Propagation.MANDATORY}, so this MUST stay
     * {@code @Transactional} — without it the call throws instead of recording.
     */
    @Transactional
    public void recordStudyDay(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        log.info("Flashcard study day: userId={}", user.getId());
        studyActivityService.recordStudy(user.getId());
    }

    @Transactional(readOnly = true)
    public Integer getStatus(Long vocabularyId, String email) {
        log.info("Lấy trạng thái flashcard: vocabularyId={}, email={}", vocabularyId, email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return progressRepository.findByUserIdAndVocabularyId(user.getId(), vocabularyId)
                .map(UserVocabularyProgress::getMasteryLevel)
                .orElse(0);
    }
}
