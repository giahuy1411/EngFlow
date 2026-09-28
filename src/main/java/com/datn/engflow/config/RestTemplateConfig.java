package com.datn.engflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Single {@link RestTemplate} bean for the two services that call blocking
 * external HTTP APIs: {@link com.datn.engflow.service.DictionaryService} (proxies
 * dictionaryapi.dev) and {@link com.datn.engflow.service.SePayApiService}
 * (payment gateway calls and webhook-status polling).
 *
 * <p>Explicit timeouts matter here because the default {@code SimpleClientHttpRequestFactory}
 * blocks indefinitely, which would pin a request thread for as long as an
 * upstream stays slow.</p>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Builds the shared HTTP client with a short connect timeout and a long read
     * timeout.
     *
     * @return a client with a 3s connect timeout and 30s read timeout
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 3s: endpoint SePay/dictionary phải bắt tay được nhanh, không có server
        // nào của ta cố ý chậm hơn thế.
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        // dictionaryapi.dev đo thực tế ~20s từ mạng VN: read timeout phải cao hơn,
        // Redis cache 1h bảo đảm mỗi từ chỉ trả giá này một lần.
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        return new RestTemplate(factory);
    }
}