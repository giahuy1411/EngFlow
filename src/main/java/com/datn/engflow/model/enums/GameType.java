package com.datn.engflow.model.enums;

/**
 * Loại mini-game luyện từ vựng, do {@code GameService} sinh đề.
 *
 * <p>Mỗi loại có luật sinh đề + chấm điểm riêng; {@code MIXED} là tổ hợp nhiều
 * loại trong một phiên. Đề được dựng trong Redis (session) chứ không lưu DB,
 * nên hết phiên là mất.
 */
public enum GameType {
    /** Lật thẻ ghi nhớ. */
    FLASHCARD,
    /** Trắc nghiệm nhanh. */
    MULTIPLE_CHOICE,
    /** Lật cặp thẻ giống nhau (memory match). */
    MEMORY_MATCH,
    /** Gõ lại từ theo nghĩa. */
    TYPING,
    /** Nghe và chọn/viết từ. */
    LISTENING,
    /** Trộn nhiều loại câu hỏi trong một phiên. */
    MIXED
}
