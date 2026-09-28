package com.datn.engflow.model.entity;

/**
 * Kiểu hoạt động mà một đề {@link SpeakingPrompt} yêu cầu người học thực hiện.
 *
 * <p>Quyết định UI có hiện khối mẫu để đọc theo hay không: frontend chỉ render
 * {@code referenceText} khi mode là {@code READ_ALOUD}. {@link SpeakingPromptService}
 * cũng dùng chính giá trị này để bắt buộc {@code READ_ALOUD} phải có
 * {@code referenceText}.
 */
public enum SpeakingPromptMode {
    /** Đọc theo một đoạn mẫu cho sẵn; chấm điểm độ khớp với mẫu. */
    READ_ALOUD,
    /** Nói tự do theo chủ đề; không có văn bản mẫu để đối chiếu. */
    FREE_SPEAKING
}
