package com.datn.engflow.model.enums;

/**
 * Độ khó của một câu bài tập, lưu ở cột {@code exercises.difficulty}.
 *
 * <p>Đây là nhãn hiển thị/lọc, <b>không</b> tham gia vào thuật toán chấm điểm
 * (điểm chỉ phụ thuộc đúng/sai). AI sinh bài cũng gán nhãn này theo prompt.
 */
public enum ExerciseDifficulty {
    /** Dễ — câu hỏi nhận biết trực tiếp. */
    EASY,
    /** Trung bình. */
    MEDIUM,
    /** Khó — câu hỏi suy luận/áp dụng. */
    HARD
}
