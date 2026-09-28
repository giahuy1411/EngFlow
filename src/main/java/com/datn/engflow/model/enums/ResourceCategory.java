package com.datn.engflow.model.enums;

/**
 * Nhóm tài nguyên học tập, dùng để phân loại bài học theo mục tiêu luyện thi.
 *
 * <p>Khác {@code SkillType}: đây là "học để thi gì", còn kỹ năng là "luyện kỹ
 * năng nào". Hai trục độc lập với nhau.
 */
public enum ResourceCategory {
    /** Nền tảng chung. */
    FOUNDATION,
    /** Luyện thi TOEIC. */
    TOEIC,
    /** Luyện thi IELTS. */
    IELTS,
    /** Luyện thi VSTEP (khung năng lực ngoại ngữ VN). */
    VSTEP
}
