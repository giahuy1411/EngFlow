package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.exception.ResourceNotFoundException;
import com.datn.engflow.model.dto.VocabularyRequest;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * class VocabularyService.
 *
 * <p>audit-v12 F147: {@code vocabulary} is a SHARED dictionary — it has no owner column,
 * and ownership of a saved word lives in {@code decks.owner_id} + {@code deck_words}. The
 * student "save a word while watching a video" path used to violate that: it wrote a row
 * straight into the shared dictionary and then made a SECOND HTTP call to link it to a
 * deck. Two problems followed:
 * <ul>
 *   <li>if the second call failed, the row was orphaned in the shared dictionary — 27 of
 *       127 rows are in no deck;</li>
 *   <li>the write was never checked against deck ownership, and the word then appeared to
 *       EVERY user through the unfiltered {@code /api/vocabulary/search} (permitAll).</li>
 * </ul>
 *
 * <p>This service makes the save a single transaction that goes THROUGH the ownership
 * layer, reusing {@link DeckService#addWordToDeck} (which already enforces ownership and is
 * idempotent) — the same pattern audit-v11 F145 established for {@code /api/ai/save-vocab}.
 */
public class VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final LessonRepository lessonRepository;
    private final DeckService deckService;

    /**
     * Create a dictionary word and, when a deck is given, link it to that deck atomically.
     *
     * @param request   the word fields
     * @param deckId    the deck to save the word into; required for non-admin callers
     * @param userId    the authenticated user (null for anonymous — rejected upstream)
     * @param isAdmin   admins may add to the shared dictionary without a deck
     * @return the persisted (or reused) vocabulary row
     */
    @Transactional
    public Vocabulary createScoped(VocabularyRequest request, Long deckId, Long userId, boolean isAdmin) {
        if (deckId == null && !isAdmin) {
            throw new BadRequestException(
                    "Cần chọn bộ từ để lưu từ vựng. Từ vựng dùng chung chỉ quản trị viên mới thêm được.");
        }

        // audit-v12 F147: dedupe. The word "negotiate" exists twice today, which makes
        // /api/vocabulary/search return duplicate results. Reusing an identical word keeps
        // the shared dictionary from growing a copy per save.
        Vocabulary vocabulary = findReusable(request)
                .orElseGet(() -> vocabularyRepository.save(build(request)));

        if (deckId != null) {
            // Ownership check + idempotency live inside addWordToDeck. Same transaction, so
            // a failure here rolls the vocabulary row back too — no more orphaned words.
            deckService.addWordToDeck(deckId, vocabulary.getId(), userId);
        }
        return vocabulary;
    }

    /**
     * Reuse an existing dictionary row when the same word is already there. Only rows that
     * are NOT attached to a lesson are reused: a lesson-scoped word is curriculum content
     * and must not be silently borrowed by a learner's deck.
     */
    private java.util.Optional<Vocabulary> findReusable(VocabularyRequest request) {
        String word = request.getWord() == null ? null : request.getWord().trim();
        if (word == null || word.isEmpty()) {
            return java.util.Optional.empty();
        }
        List<Vocabulary> matches = vocabularyRepository.findByWordContainingIgnoreCase(word);
        return matches.stream()
                .filter(v -> v.getWord() != null && v.getWord().equalsIgnoreCase(word))
                .filter(v -> v.getLesson() == null)
                .findFirst();
    }

    private Vocabulary build(VocabularyRequest request) {
        // audit-v12 F147: the old controller ignored lessonId entirely (a comment said
        // "skipped for simplicity"), so a caller that supplied one silently got a
        // lesson-less word. Resolve it the same way AdminService.createVocabulary does.
        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        }
        return Vocabulary.builder()
                .word(request.getWord())
                .pronunciation(request.getPronunciation())
                .meaning(request.getMeaning())
                .exampleSentence(request.getExampleSentence())
                .audioUrl(request.getAudioUrl())
                .imageUrl(request.getImageUrl())
                .wordType(request.getWordType())
                .definitionEn(request.getDefinitionEn())
                .cefrLevel(request.getCefrLevel())
                .source(request.getSource())
                .lesson(lesson)
                .build();
    }
}
