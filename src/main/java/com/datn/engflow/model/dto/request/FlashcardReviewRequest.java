package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * class FlashcardReviewRequest.
 */
public class FlashcardReviewRequest {
    @NotNull(message = "ID từ vựng không được để trống")
    private Long vocabularyId;

    @NotNull(message = "Trạng thái ghi nhớ không được để trống")
    private Boolean isKnown; // true for "Đã thuộc", false for "Chưa thuộc"
}
