package com.datn.engflow.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Phiên game tạm, lưu trong Redis để chấm điểm server-side thay vì tin số liệu client gửi lên.
 *
 * <p>{@code GameService} ghi đối tượng này vào key {@code game:session:<sessionId>} với TTL
 * {@link com.datn.engflow.config.RedisConstants#GAME_SESSION_TTL}; khi nộp bài,
 * {@code GameService.submitGameResult} đọc lại và so từng câu với {@link #answerMap}. Nếu
 * client không gửi đáp án thì mới rơi về nhánh tin client, kèm trần điểm ngày.
 *
 * <p>Redis dùng {@code GenericJackson2JsonRedisSerializer}, nên {@code Serializable} và
 * {@code serialVersionUID} ở đây là để tuân thủ serialization của Java, không phải để Redis
 * dùng tới.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionRedisDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private Long userId;
    private Long deckId;
    private String gameType;
    private Integer totalQuestions;
    // Stores correct answers for server-side validation (vocabId -> meaning or word -> meaning)
    private java.util.Map<String, String> answerMap;
}
