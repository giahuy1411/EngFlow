package com.datn.engflow.model.dto.projection;

/**
 * Projection chỉ gồm id + tiêu đề bài học. Dùng cho trang admin exercise để
 * dán nhãn từng dòng mà KHÔNG nạp {@code content} / {@code content_original}
 * (đo được: nạp cả lesson kèm LOB tốn 320 logical reads và 32ms mỗi trang 20
 * dòng, so với 0 và 1ms khi chỉ chọn tiêu đề).
 */
public interface LessonTitle {

    /** @return id bài học. */
    Long getLessonId();

    /** @return tiêu đề bài học. */
    String getTitle();
}
