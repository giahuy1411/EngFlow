package com.datn.engflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for polling SePay's User API to check if a payment has been recorded.
 * Used as a fallback when the SePay webhook callback cannot reach the
 * local server (e.g. localhost / Docker development environment).
 *
 * <p>Endpoint and response shape follow the official docs:
 * <a href="https://docs.sepay.vn/api-giao-dich.html">API Giao dịch</a> —
 * {@code GET https://my.sepay.vn/userapi/transactions/list} with an
 * {@code amount_in} filter and {@code Authorization: Bearer <token>}.</p>
 */
@Service
@Slf4j
public class SePayApiService {

    /**
     * User API list endpoint per official docs. The previous host
     * (userapi.sepay.vn/v2) is not in the documentation and its response
     * shape could not be verified.
     */
    private static final String TRANSACTIONS_LIST_URL =
            "https://my.sepay.vn/userapi/transactions/list";

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
            log.warn("SEPAY_API_TOKEN is NOT configured! SePay webhook fallback polling will be DISABLED.");
            log.warn("Set SEPAY_API_TOKEN in .env (my.sepay.vn -> Cau hinh Cong ty -> API Access -> + Them API), then rebuild Docker.");
            log.warn("Without it, manual bank transfers will NOT be detected automatically.");
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
     * Query the SePay User API for an incoming transaction whose content
     * matches the given order code and whose amount matches exactly.
     *
     * <p>The {@code amount_in} filter matches on exact incoming amount; the
     * order-code check is done client-side against {@code transaction_content}
     * (docs expose no content filter). Response keys are read defensively in
     * both documented and legacy shapes and normalized before use.</p>
     *
     * @param orderCode the order code (e.g. ENGXXXXXXXXXXXX) to search for in transaction content
     * @param amount    the expected transfer amount
     * @return an Optional containing the normalized matching transaction map, or empty if not found
     */
    public Optional<Map<String, Object>> findTransactionByOrderCode(String orderCode, BigDecimal amount) {
        if (apiToken == null || apiToken.isBlank()) {
            log.warn("SePay API token not configured, cannot poll for transactions");
            return Optional.empty();
        }

        String url = TRANSACTIONS_LIST_URL
                + "?amount_in=" + amount.toBigInteger()
                + "&limit=20";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }

            Map<String, Object> result = objectMapper.readValue(body, new TypeReference<>() {});
            List<Map<String, Object>> transactions = readTransactionList(result);

            if (transactions == null || transactions.isEmpty()) {
                log.debug("SePay API returned no transactions for order code: {}", orderCode);
                return Optional.empty();
            }

            for (Map<String, Object> tx : transactions) {
                String content = readContent(tx);
                if (content != null && content.toUpperCase().contains(orderCode.toUpperCase())) {
                    log.info("SePay API found matching transaction for order code: {}", orderCode);
                    return Optional.of(normalizeTransaction(tx));
                }
            }

            log.debug("SePay API returned transactions but none match order code: {}", orderCode);
            return Optional.empty();

        } catch (HttpClientErrorException.TooManyRequests e) {
            String retryAfter = e.getResponseHeaders() != null
                    ? e.getResponseHeaders().getFirst("x-sepay-userapi-retry-after") : null;
            log.warn("SePay API rate limited (429), retry-after={}s; will be retried on next status poll", retryAfter);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("SePay API polling failed for order code {}: {}", orderCode, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Read the transaction list from the API response. The documented shape is
     * {@code { transactions: [...] }}; the legacy shape {@code { data: [...] }}
     * is accepted so a silent SePay-side change degrades instead of breaking.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readTransactionList(Map<String, Object> result) {
        Object list = result.get("transactions");
        if (list == null) {
            list = result.get("data");
        }
        return list instanceof List ? (List<Map<String, Object>>) list : null;
    }

    /**
     * Read the transfer content from a transaction row. Docs expose
     * {@code transaction_content}; legacy rows may carry {@code content}.
     */
    private String readContent(Map<String, Object> tx) {
        Object content = tx.get("transaction_content");
        if (content == null) {
            content = tx.get("content");
        }
        return content != null ? String.valueOf(content) : null;
    }

    /**
     * Normalize a raw API transaction row into the shape the payment
     * processor expects: {@code id}, {@code content}, {@code amount_in} /
     * {@code transferAmount}, {@code gateway}.
     */
    private Map<String, Object> normalizeTransaction(Map<String, Object> tx) {
        Map<String, Object> normalized = new java.util.HashMap<>();
        normalized.put("id", tx.get("id"));
        normalized.put("content", readContent(tx));
        Object amountIn = tx.get("amount_in");
        normalized.put("transferAmount", amountIn != null ? amountIn : tx.get("transferAmount"));
        Object gateway = tx.get("bank_brand_name");
        normalized.put("gateway", gateway != null ? gateway : tx.get("gateway"));
        return normalized;
    }
}
