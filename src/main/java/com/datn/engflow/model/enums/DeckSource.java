package com.datn.engflow.model.enums;

/**
 * Nguồn gốc của một bộ từ (deck) — dùng để phân biệt bộ hệ thống (seed sẵn theo
 * giáo trình) với bộ do người dùng hoặc AI tạo.
 *
 * <p>Giá trị lưu thẳng vào cột {@code decks.source} dạng chuỗi enum, nên
 * <b>đổi tên một hằng số = đổi dữ liệu DB</b>: phải migrate cột trước. Bộ seed
 * (OXFORD/AWL/TOEIC/IELTS/THPT) do {@code VocabularyDataSeeder} tạo; phần còn
 * lại do người dùng thao tác.
 */
public enum DeckSource {
    /** Bộ 3000 từ Oxford thông dụng. */
    OXFORD3000,
    /** Bộ 5000 từ Oxford mở rộng. */
    OXFORD5000,
    /** Academic Word List — từ vựng học thuật. */
    AWL,
    /** Từ vựng luyện thi TOEIC. */
    TOEIC,
    /** Từ vựng luyện thi IELTS. */
    IELTS,
    /** Từ vựng luyện thi THPT Quốc gia. */
    THPT,
    /** Bộ do AI (Ollama) sinh ra qua {@code AiVocabService}. */
    AI_GENERATED,
    /** Bộ do chính người học tạo tay. */
    USER_CREATED,
    /** audit-v12 F147: từ người học lưu lại trong lúc xem bài video. */
    VIDEO_LESSON
}
