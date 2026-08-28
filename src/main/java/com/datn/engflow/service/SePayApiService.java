package com.datn.engflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Service for polling SePay's API to check if a payment has been recorded.
 * Used as a fallback when the SePay webhook callback cannot reach the
 * local server (e.g. localhost / Docker development environment).
 */
@Service
@Slf4j
public class SePayApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiToken;

    public SePayApiService(RestTemplate restTemplate,
                           ObjectMapper objectMapper,
                           @Value("${sepay.api-token:}") String apiToken) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiToken = apiToken;
        if (apiToken == null || apiToken.isBlank()) {
            log.warn("⚠️  SEPAY_API_TOKEN is NOT configured! SePay webhook fallback polling will be DISABLED.");
            log.warn("⚠️  Set SEPAY_API_TOKEN in .env (get at https://app.sepay.vn/ -> Cài đặt -> API), then rebuild Docker.");
            log.warn("⚠️  Without it, manual bank transfers will NOT be detected automatically.");
        } else {
            log.info("SePay API token configured. Webhook fallback polling is ENABLED.");
        }
    }

    /**
     * Check whether the SePay API token is configured.
     *
     * @return true if the API token is present and non-blank
     */
    public boolean isTokenConfigured() {
        return apiToken != null && !apiToken.isBlank();
    }

    /**
     * Query the SePay transaction API for a transaction matching the given
     * order code (in the content field) and amount.
     *
     * @param orderCode the order code (e.g. ENGXXXXXXXXXXXX) to search for in transaction content
     * @param amount    the expected transfer amount
     * @return an Optional containing the matching transaction map, or empty if not found
     */
    public Optional<Map<String, Object>> findTransactionByOrderCode(String orderCode, BigDecimal amount) {
        if (apiToken == null || apiToken.isBlank()) {
            log.warn("SePay API token not configured, cannot poll for transactions");
            return Optional.empty();
        }

        String url = "https://userapi.sepay.vn/v2/transactions"
                + "?q=" + orderCode
                + "&amount_in_min=" + amount.toBigInteger()
                + "&amount_in_max=" + amount.toBigInteger()
                + "&limit=20";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }

            Map<String, Object> result = objectMapper.readValue(body, new TypeReference<>() {});
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("data");

            if (transactions == null || transactions.isEmpty()) {
                log.debug("SePay API returned no transactions for order code: {}", orderCode);
                return Optional.empty();
            }

            for (Map<String, Object> tx : transactions) {
                String content = (String) tx.getOrDefault("content", "");
                if (content != null && content.toUpperCase().contains(orderCode.toUpperCase())) {
                    log.info("SePay API found matching transaction for order code: {}", orderCode);
                    return Optional.of(tx);
                }
            }

            log.debug("SePay API returned transactions but none match order code: {}", orderCode);
            return Optional.empty();

        } catch (Exception e) {
            log.warn("SePay API polling failed for order code {}: {}", orderCode, e.getMessage());
            return Optional.empty();
        }
    }
}
