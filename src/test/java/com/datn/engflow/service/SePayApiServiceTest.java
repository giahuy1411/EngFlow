package com.datn.engflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SePayApiService} — the polling fallback that queries the
 * SePay User API when the webhook cannot reach the local server. Response
 * shapes follow the official docs (docs.sepay.vn/api-giao-dich.html).
 */
@ExtendWith(MockitoExtension.class)
class SePayApiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SePayApiService sePayApiService;

    private final String apiToken = "test-api-token";
    private static final String LIST_URL = "https://my.sepay.vn/userapi/transactions/list";

    @BeforeEach
    void setUp() {
        sePayApiService = new SePayApiService(restTemplate, objectMapper, apiToken);
    }

    private void stubResponse(String json) {
        when(restTemplate.exchange(eq(LIST_URL + "?amount_in=10000&limit=20"),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));
    }

    private String documentedResponse(String id, String content) {
        // Shape per docs.sepay.vn/api-giao-dich.html
        return "{\"status\":200,\"error\":null,\"messages\":{\"success\":true},"
                + "\"transactions\":[{"
                + "\"id\":\"" + id + "\","
                + "\"bank_brand_name\":\"MBBank\","
                + "\"account_number\":\"0706718329\","
                + "\"transaction_date\":\"2026-08-29 10:00:00\","
                + "\"amount_out\":\"0.00\","
                + "\"amount_in\":\"10000.00\","
                + "\"accumulated\":\"0.00\","
                + "\"transaction_content\":\"" + content + "\","
                + "\"reference_number\":\"FT26241824402913\""
                + "}]}";
    }

    @Test
    void findTransactionByOrderCode_documentedShape_matchesAndNormalizes() throws Exception {
        // Given - response exactly as documented
        stubResponse(documentedResponse("77352516", "ENGABCDEF123456 thanh toan"));

        // When
        Optional<Map<String, Object>> result =
                sePayApiService.findTransactionByOrderCode("ENGABCDEF123456", new BigDecimal("10000"));

        // Then - normalized to the shape PaymentService.checkPendingPayments consumes
        assertThat(result).isPresent();
        Map<String, Object> tx = result.get();
        assertThat(tx.get("id")).isEqualTo("77352516");
        assertThat(tx.get("content")).isEqualTo("ENGABCDEF123456 thanh toan");
        assertThat(tx.get("transferAmount")).isEqualTo("10000.00");
        assertThat(tx.get("gateway")).isEqualTo("MBBank");
    }

    @Test
    void findTransactionByOrderCode_contentMatchIsCaseInsensitive() throws Exception {
        // Given - bank content arrives lowercase
        stubResponse(documentedResponse("77352517", "chuyen tien engabcdef123456"));

        // When
        Optional<Map<String, Object>> result =
                sePayApiService.findTransactionByOrderCode("ENGABCDEF123456", new BigDecimal("10000"));

        // Then
        assertThat(result).isPresent();
    }

    @Test
    void findTransactionByOrderCode_legacyDataShape_stillParsed() throws Exception {
        // Given - legacy shape with "data" list and "content" key
        String legacy = "{\"status\":200,\"data\":[{"
                + "\"id\":\"100\",\"content\":\"ENGABCDEF123456\",\"amount_in\":\"10000.00\",\"gateway\":\"MB\""
                + "}]}";
        stubResponse(legacy);

        // When
        Optional<Map<String, Object>> result =
                sePayApiService.findTransactionByOrderCode("ENGABCDEF123456", new BigDecimal("10000"));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().get("id")).isEqualTo("100");
    }

    @Test
    void findTransactionByOrderCode_emptyList_returnsEmpty() throws Exception {
        // Given
        stubResponse("{\"status\":200,\"error\":null,\"messages\":{\"success\":true},\"transactions\":[]}");

        // When
        Optional<Map<String, Object>> result =
                sePayApiService.findTransactionByOrderCode("ENGABCDEF123456", new BigDecimal("10000"));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findTransactionByOrderCode_transactionsKeyMissing_returnsEmpty() throws Exception {
        // Given - unexpected shape (neither transactions nor data)
        stubResponse("{\"status\":200,\"error\":\"not found\"}");

        // When
        Optional<Map<String, Object>> result =
                sePayApiService.findTransactionByOrderCode("ENGABCDEF123456", new BigDecimal("10000"));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findTransactionByOrderCode_noContentMatch_returnsEmpty() throws Exception {
        // Given - amount matches but content carries a different order code
        stubResponse(documentedResponse("77352518", "ENGZZZZZZZZZZZZ thanh toan"));

        // When
        Optional<Map<String, Object>> result =
                sePayApiService.findTransactionByOrderCode("ENGABCDEF123456", new BigDecimal("10000"));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findTransactionByOrderCode_httpError_returnsEmptyWithoutThrow() {
        // Given - SePay returns 500
        when(restTemplate.exchange(eq(LIST_URL + "?amount_in=10000&limit=20"),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection reset"));

        // When
        Optional<Map<String, Object>> result =
                sePayApiService.findTransactionByOrderCode("ENGABCDEF123456", new BigDecimal("10000"));

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findTransactionByOrderCode_rateLimited_returnsEmptyWithLog() {
        // Given - 429 per docs rate limit (3 req/s)
        when(restTemplate.exchange(eq(LIST_URL + "?amount_in=10000&limit=20"),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.TooManyRequests.create(
                        HttpStatus.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        new HttpHeaders(), null, null));

        // When
        Optional<Map<String, Object>> result =
                sePayApiService.findTransactionByOrderCode("ENGABCDEF123456", new BigDecimal("10000"));

        // Then - swallowed so the status poll keeps working
        assertThat(result).isEmpty();
    }

    @Test
    void findTransactionByOrderCode_tokenNotConfigured_skipsHttpCall() {
        // Given - service constructed without a token
        SePayApiService noTokenService = new SePayApiService(restTemplate, objectMapper, "");

        // When
        Optional<Map<String, Object>> result =
                noTokenService.findTransactionByOrderCode("ENGABCDEF123456", new BigDecimal("10000"));

        // Then - no HTTP request is made
        assertThat(result).isEmpty();
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void isTokenConfigured_reflectsConstructorState() {
        assertThat(sePayApiService.isTokenConfigured()).isTrue();
        SePayApiService noTokenService = new SePayApiService(restTemplate, objectMapper, "  ");
        assertThat(noTokenService.isTokenConfigured()).isFalse();
    }
}
