package com.datn.engflow.service;

/**
 * Trừu tượng hoá text-to-speech để sinh audio cho câu hỏi mà bài LISTENING sẽ
 * đọc. Trong dự án chỉ có một implementation là
 * {@link SupertonicProxyTtsService}, một proxy tới sidecar supertonic (cổng
 * 8001); interface tồn tại để có thể thay bằng nhà cung cấp TTS đám mây mà không
 * phải sửa bên gọi. Class {@code CloudTtsService} cũ đã bị xoá.
 */
public interface TtsService {

    /**
     * Kết xuất {@code text} thành audio.
     *
     * @param text  văn bản cần đọc
     * @param voice id giọng đọc, hoặc null để backend tự chọn mặc định
     * @param lang  gợi ý ngôn ngữ, hoặc null để backend dùng mặc định
     * @return byte audio, hoặc null khi tổng hợp thất bại hay không khả dụng
     */
    byte[] synthesize(String text, String voice, String lang);

    /**
     * Dò xem backend đọc tiếng hiện có tới được không.
     *
     * @return true khi một request tổng hợp tại thời điểm này sẽ thành công
     */
    boolean isAvailable();
}
