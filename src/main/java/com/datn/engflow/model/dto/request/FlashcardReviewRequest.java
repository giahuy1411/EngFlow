package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * class FlashcardReviewRequest.
 *
 * <p>audit-v12 F148: this used to carry a binary {@code isKnown} flag, which collapsed the
 * UI's three rating buttons ("Lại" / "Tiếp theo" / "Dễ") into two values — "Dễ" and
 * "Tiếp theo" sent an identical request, so the extra signal the learner gave was thrown
 * away. It now carries the SM-2 quality (0-5) that {@code SrsService.reviewWord} consumes,
 * so all three buttons mean something different.
 */
public class FlashcardReviewRequest {
    @NotNull(message = "ID từ vựng không được để trống")
    private Long vocabularyId;

    /**
     * SM-2 quality, 0-5. The UI maps: "Lại" -&gt; 1, "Tiếp theo" -&gt; 4, "Dễ" -&gt; 5.
     * Any value below 3 resets the repetition chain (the word is treated as not recalled).
     */
    @NotNull(message = "Mức ghi nhớ không được để trống")
    @Min(value = 0, message = "quality phải từ 0 đến 5")
    @Max(value = 5, message = "quality phải từ 0 đến 5")
    private Integer quality;
}
