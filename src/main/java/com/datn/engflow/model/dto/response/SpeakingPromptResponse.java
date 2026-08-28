package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.entity.SpeakingPromptMode;
import com.datn.engflow.model.entity.SpeakingPrompt;

import java.time.LocalDateTime;

/**
 * Public representation of a Speaking Coach prompt.
 */
public record SpeakingPromptResponse(
        Long id,
        String title,
        String description,
        String prompt,
        Long lessonId,
        String lessonTitle,
        SpeakingPromptMode mode,
        String referenceText,
        String referenceMediaUrl,
        Integer maxDurationSeconds,
        Integer attemptLimit,
        String level,
        String category,
        Boolean isPremium,
        String thumbnailUrl,
        Integer orderIndex,
        Boolean isPublished,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Maps the persistence model to a stable response contract.
     *
     * @param entity prompt entity
     * @return public response
     */
    public static SpeakingPromptResponse from(SpeakingPrompt entity) {
        return new SpeakingPromptResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getPrompt(),
                entity.getLesson() == null ? null : entity.getLesson().getId(),
                entity.getLesson() == null ? null : entity.getLesson().getTitle(),
                entity.getMode(),
                entity.getReferenceText(),
                entity.getReferenceMediaUrl() != null ? entity.getReferenceMediaUrl().replace("minio:9000", "localhost:9000") : null,
                entity.getMaxDurationSeconds(),
                entity.getAttemptLimit(),
                entity.getLevel(),
                entity.getCategory(),
                entity.getIsPremium(),
                entity.getThumbnailUrl(),
                entity.getOrderIndex(),
                entity.getIsPublished(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
