package com.datn.engflow.model.enums;

/**
 * Kỹ năng chính của một bài học, lưu ở cột {@code lessons.skill_type}.
 *
 * <p>Quyết định component nào render trong tab "Nội dung" của
 * {@code LessonLayout.vue} (mỗi giá trị ánh xạ tới một {@code *Skill.vue}).
 * Thêm giá trị mới phải thêm cả component frontend tương ứng.
 */
public enum SkillType {
    /** Từ vựng. */
    VOCABULARY,
    /** Ngữ pháp. */
    GRAMMAR,
    /** Nghe. */
    LISTENING,
    /** Đọc. */
    READING,
    /** Viết. */
    WRITING,
    /** Nói. */
    SPEAKING,
    /** Kỹ năng từ (word skills: collocation, word form...). */
    WORD_SKILLS
}
