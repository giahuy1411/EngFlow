package com.datn.engflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Công bố {@link ObjectMapper} Jackson dùng chung cho toàn app để đọc/ghi JSON
 * (body request tới AI, response của LLM, file seed của crawler).
 *
 * <p>Bean được đánh {@code @Primary} nên thắng mapper mà Jackson auto-configuration
 * của Spring Boot đóng góp, nhờ đó cả context dùng chung một policy serialize duy
 * nhất. Nhiều service inject nó qua constructor — {@code AiExerciseService},
 * {@code JsonDataSeeder} và
 * {@link com.datn.engflow.service.assessment.SpeakingAssessmentService} — trong khi
 * vài code path cũ tự tạo {@code new ObjectMapper()} dùng một lần rồi bỏ; mấy chỗ
 * đó cố ý né cấu hình này.</p>
 *
 * <p>Lưu ý về date/time: {@code jackson-datatype-jsr310} là dependency KHAI BÁO
 * TƯỜNG MINH trong pom (audit-v12 C6). Trước đây nó chỉ có trên classpath một cách
 * bắc cầu qua Azure Speech SDK; gỡ SDK đó ra là JavaTimeModule biến mất âm thầm và
 * test date với {@code LocalDateTime} fail. Đừng gỡ dependency này.</p>
 */
@Configuration
public class JacksonConfig {

    /**
     * Đưa ra một {@link ObjectMapper} cấu hình mặc định làm bean primary.
     *
     * <p>Không đăng ký feature nào ở đây: parse dùng default của thư viện. Cần chú
     * ý vì {@code new ObjectMapper()} KHÔNG đi qua {@code Jackson2ObjectMapperBuilder}
     * của Spring Boot, nên các cờ auto-config thường có (ví dụ bỏ qua property lạ)
     * không được áp dụng — property lạ trong JSON vẫn fail. Caller nào muốn nới thì
     * phải tự cấu hình mapper riêng.</p>
     *
     * @return mapper primary dùng chung cho mọi consumer Jackson
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
