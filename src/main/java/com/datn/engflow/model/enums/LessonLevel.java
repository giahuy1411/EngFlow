package com.datn.engflow.model.enums;

/**
 * Trình độ của một bài học, lưu ở cột {@code lessons.level}.
 *
 * <p>Frontend map sang nhãn tiếng Việt + màu badge trong {@code utils/lessonLevels.js};
 * thêm giá trị mới phải cập nhật cả map đó, nếu không badge sẽ trắng.
 */
public enum LessonLevel {
    /** Sơ cấp (A1–A2). */
    ELEMENTARY,
    /** Tiền trung cấp (A2–B1). */
    PRE_INTERMEDIATE,
    /** Trung cấp (B1–B2). */
    INTERMEDIATE,
    /** Trung cao cấp (B2–C1). */
    UPPER_INTERMEDIATE
}
