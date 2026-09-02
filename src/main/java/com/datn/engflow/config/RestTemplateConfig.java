package com.datn.engflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for HTTP clients used by services that need to call
 * external APIs (e.g. SePay API polling fallback, dictionary proxy).
 * Timeouts prevent worker threads from hanging up to 20s+ when an
 * upstream API is slow or unreachable.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        // dictionaryapi.dev đo thực tế ~20s từ mạng VN: read timeout phải cao hơn,
        // Redis cache 1h bảo đảm mỗi từ chỉ trả giá này một lần.
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        return new RestTemplate(factory);
    }
}