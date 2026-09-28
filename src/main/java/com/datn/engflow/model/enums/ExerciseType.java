package com.datn.engflow.model.enums;

/**
 * Loại câu bài tập, lưu ở cột {@code exercises.exercise_type}.
 *
 * <p><b>Hợp đồng quan trọng:</b> mỗi loại có format {@code options} và
 * {@code correct_answer} riêng, phải khớp với frontend:
 * <ul>
 *   <li>{@code MATCHING}: options là các cặp {@code "left|right"} (phân tách bằng
 *       dấu {@code |}), đáp án là {@code "l=r,l=r,..."}. Dấu {@code :::} của
 *       prompt AI cũ <b>đã bị gỡ</b> — xem {@code AiExerciseService} phần repair.</li>
 *   <li>{@code MULTIPLE_CHOICE}: đáp án là nội dung phương án, <b>không</b> phải
 *       ký tự "A"/"B" (guard chống bare-letter trong {@code ExerciseService}).</li>
 *   <li>{@code LISTENING}: cần {@code audio_url}; thiếu thì UI fallback giọng trình duyệt.</li>
 * </ul>
 */
public enum ExerciseType {
    /** Trắc nghiệm — chọn 1 phương án đúng. */
    MULTIPLE_CHOICE,
    /** Điền vào chỗ trống. */
    FILL_BLANK,
    /** Nghe hiểu — cần {@code audio_url}. */
    LISTENING,
    /** Nối cặp (left|right) — xem hợp đồng ở Javadoc trên. */
    MATCHING,
    /** Dịch câu. */
    TRANSLATION
}
