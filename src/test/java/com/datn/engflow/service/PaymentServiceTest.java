package com.datn.engflow.service;

import com.datn.engflow.model.entity.PaymentTransaction;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.PaymentTransactionRepository;
import com.datn.engflow.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SePayApiService sePayApiService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<PaymentTransaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private final String webhookSecret = "test-webhook-secret-123";
    private final String bankAccount = "123456789";
    private final String bankName = "VCB";
    private final String sepayQrUrl = "https://qr.sepay.vn/img";

    private User baseUser(Long id) {
        return User.builder()
                .id(id)
                .username("user" + id)
                .email("user" + id + "@test.com")
                .passwordHash("hash")
                .isPremium(false)
                .totalPoints(0)
                .build();
    }

    private PaymentTransaction pendingTx(String orderCode, String planType, Long userId) {
        User u = baseUser(userId);
        return PaymentTransaction.builder()
                .id(1L)
                .orderCode(orderCode)
                .amount(new BigDecimal("10000"))
                .planType(planType)
                .status("PENDING")
                .user(u)
                .build();
    }

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentTransactionRepository, userRepository, objectMapper, sePayApiService);
        ReflectionTestUtils.setField(paymentService, "webhookSecret", webhookSecret);
        ReflectionTestUtils.setField(paymentService, "sepayQrUrl", sepayQrUrl);
        ReflectionTestUtils.setField(paymentService, "bankAccount", bankAccount);
        ReflectionTestUtils.setField(paymentService, "bankName", bankName);
    }

    // ---- helpers for webhook HMAC ----

    private String baseJsonWithoutSignature(String orderCode) throws Exception {
        // compact JSON to keep stripSignature deterministic
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", 12345);
        body.put("content", "Chuyen tien " + orderCode + " thanh toan");
        body.put("amount_in", "10000");
        body.put("gateway", "VCB");
        return objectMapper.writeValueAsString(body);
    }

    private String buildRawBodyWithSignature(String baseJson, String signature) {
        // insert signature before final }
        return baseJson.substring(0, baseJson.length() - 1) + ",\"signature\":\"" + signature + "\"}";
    }

    private String computeExpectedSig(String strippedBody) {
        return HmacUtils.hmacSha256Hex(webhookSecret, strippedBody);
    }

    // ---- processWebhook ----

    @Test
    void processWebhook_bankContentWithoutUnderscore_matchesNewOrderFormat() throws Exception {
        // Given - the VietQR generator strips "_" from the des= parameter, so content
        // arriving from a QR-initiated transfer is underscore-free (ENGABCDEF123456).
        String orderCode = "ENGABCDEF123456";
        String baseJson = baseJsonWithoutSignature(orderCode);
        String expectedSig = computeExpectedSig(baseJson);
        String rawBody = buildRawBodyWithSignature(baseJson, expectedSig);

        User user = baseUser(14L);
        PaymentTransaction pending = pendingTx(orderCode, "MONTH", 14L);
        pending.setUser(user);

        when(paymentTransactionRepository.findByTransactionId("12345")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(rawBody);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void processWebhook_contentWithoutUnderscore_fallsBackToLegacyUnderscoreOrder() throws Exception {
        // Given - QR content arrives underscore-free but the PENDING row was created
        // before the format change and stores "ENG_ABCDEF123456". The legacy fallback
        // must resolve it so in-flight orders are not orphaned.
        String legacyOrderCode = "ENG_ABCDEF123456";
        String strippedCode = "ENGABCDEF123456";
        String baseJson = baseJsonWithoutSignature(strippedCode);
        String expectedSig = computeExpectedSig(baseJson);
        String rawBody = buildRawBodyWithSignature(baseJson, expectedSig);

        User user = baseUser(16L);
        PaymentTransaction pending = pendingTx(legacyOrderCode, "MONTH", 16L);
        pending.setUser(user);

        when(paymentTransactionRepository.findByTransactionId("12345")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(strippedCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(strippedCode, "PENDING")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(legacyOrderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(rawBody);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void processWebhook_bankContentLowercase_matchesOrderCaseInsensitively() throws Exception {
        // Given - bank content may arrive lowercase; matcher uppercases before matching.
        String orderCode = "ENGABCDEF123456";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", 12345);
        body.put("content", "chuyen tien " + orderCode.toLowerCase() + " thanh toan");
        body.put("amount_in", "10000");
        body.put("gateway", "VCB");
        String baseJson = objectMapper.writeValueAsString(body);
        String expectedSig = computeExpectedSig(baseJson);
        String rawBody = buildRawBodyWithSignature(baseJson, expectedSig);

        User user = baseUser(15L);
        PaymentTransaction pending = pendingTx(orderCode, "MONTH", 15L);
        pending.setUser(user);

        when(paymentTransactionRepository.findByTransactionId("12345")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(rawBody);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
    }

    @Test
    void processWebhook_validSignature_processesTransaction() throws Exception {
        // Given
        String orderCode = "ENGABCDEF123456";
        String baseJson = baseJsonWithoutSignature(orderCode);
        String expectedSig = computeExpectedSig(baseJson);
        String rawBody = buildRawBodyWithSignature(baseJson, expectedSig);

        User user = baseUser(1L);
        PaymentTransaction pending = pendingTx(orderCode, "MONTH", 1L);
        pending.setUser(user);

        when(paymentTransactionRepository.findByTransactionId("12345")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(rawBody);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        PaymentTransaction saved = transactionCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getTransactionId()).isEqualTo("12345");
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getIsPremium()).isTrue();
        assertThat(userCaptor.getValue().getPremiumExpiry()).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    void processWebhook_validSignatureInHeader_processesTransactionWithoutBodySignature() throws Exception {
        // Given - SePay chuÃ¡ÂºÂ©n gÃ¡Â»Â­i signature Ã¡Â»Å¸ header X-Signature, KHÃƒâ€NG cÃƒÂ³ trong body.
        String orderCode = "ENGABCDEF123456";
        String baseJson = baseJsonWithoutSignature(orderCode);
        String expectedSig = computeExpectedSig(baseJson);
        // body khÃƒÂ´ng chÃ¡Â»Â©a field signature
        String rawBody = baseJson;

        User user = baseUser(7L);
        PaymentTransaction pending = pendingTx(orderCode, "MONTH", 7L);
        pending.setUser(user);

        when(paymentTransactionRepository.findByTransactionId("12345")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // When - signature chÃ¡Â»â€° Ã¡Â»Å¸ header
        Map<String, Object> actualResult = paymentService.processWebhook(rawBody, expectedSig);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getIsPremium()).isTrue();
    }

    @Test
    void processWebhook_wrongHeaderSignature_rejects() throws Exception {
        // Given - signature header sai
        String orderCode = "ENGABCDEF123456";
        String baseJson = baseJsonWithoutSignature(orderCode);
        String rawBody = baseJson;

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(rawBody, "wrong-header-signature");

        // Then
        assertThat(actualResult.get("success")).isEqualTo(false);
        assertThat(actualResult.get("error")).isEqualTo("Invalid signature");
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void processWebhook_sepayProductionSignature_processesTransaction() throws Exception {
        // Given - SePay production: X-Sepay-Signature: sha256=<hex>, signed over "<ts>.<rawBody>"
        // Timestamp must be within the Â±5 min replay window, so use current time.
        String orderCode = "ENGABCDEF123456";
        String rawBody = baseJsonWithoutSignature(orderCode);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = HmacUtils.hmacSha256Hex(webhookSecret, timestamp + "." + rawBody);

        User user = baseUser(9L);
        PaymentTransaction pending = pendingTx(orderCode, "MONTH", 9L);
        pending.setUser(user);

        when(paymentTransactionRepository.findByTransactionId("12345")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(
                rawBody, null, "sha256=" + signature, timestamp);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getIsPremium()).isTrue();
    }

    @Test
    void processWebhook_sepayProductionWrongSignature_rejects() throws Exception {
        // Given - production header present but signature does not match
        String orderCode = "ENGABCDEF123456";
        String rawBody = baseJsonWithoutSignature(orderCode);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(
                rawBody, null, "sha256=deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef", timestamp);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(false);
        assertThat(actualResult.get("error")).isEqualTo("Invalid signature");
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void processWebhook_sepayProductionSignatureWithoutPrefix_processesTransaction() throws Exception {
        // Given - same as production scheme but header value without "sha256=" prefix
        String orderCode = "ENGABCDEF123456";
        String rawBody = baseJsonWithoutSignature(orderCode);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = HmacUtils.hmacSha256Hex(webhookSecret, timestamp + "." + rawBody);

        User user = baseUser(11L);
        PaymentTransaction pending = pendingTx(orderCode, "MONTH", 11L);
        pending.setUser(user);

        when(paymentTransactionRepository.findByTransactionId("12345")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(
                rawBody, null, signature, timestamp);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
    }

    @Test
    void processWebhook_staleTimestamp_rejectsReplay() throws Exception {
        // Given - valid production signature but timestamp 10 minutes old.
        // Docs (developer.sepay.vn/vi/sepay-webhooks/xac-thuc): reject if
        // abs(time() - timestamp) > 300 seconds, so captured payloads cannot
        // be replayed indefinitely.
        String orderCode = "ENGABCDEF123456";
        String rawBody = baseJsonWithoutSignature(orderCode);
        String staleTimestamp = String.valueOf(Instant.now().getEpochSecond() - 600);
        String signature = HmacUtils.hmacSha256Hex(webhookSecret, staleTimestamp + "." + rawBody);

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(
                rawBody, null, "sha256=" + signature, staleTimestamp);

        // Then - rejected before any repository interaction
        assertThat(actualResult.get("success")).isEqualTo(false);
        assertThat(actualResult.get("error")).isEqualTo("Invalid signature");
        verify(paymentTransactionRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void processWebhook_futureTimestamp_rejectsReplay() throws Exception {
        // Given - valid signature but timestamp 10 minutes in the future
        // (also outside the window: an attacker can shift timestamps forward too).
        String orderCode = "ENGABCDEF123456";
        String rawBody = baseJsonWithoutSignature(orderCode);
        String futureTimestamp = String.valueOf(Instant.now().getEpochSecond() + 600);
        String signature = HmacUtils.hmacSha256Hex(webhookSecret, futureTimestamp + "." + rawBody);

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(
                rawBody, null, "sha256=" + signature, futureTimestamp);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(false);
        assertThat(actualResult.get("error")).isEqualTo("Invalid signature");
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void processWebhook_nonNumericTimestamp_rejects() throws Exception {
        // Given - garbage timestamp header (tampered or malformed)
        String orderCode = "ENGABCDEF123456";
        String rawBody = baseJsonWithoutSignature(orderCode);

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(
                rawBody, null, "sha256=abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd", "not-a-number");

        // Then
        assertThat(actualResult.get("success")).isEqualTo(false);
        assertThat(actualResult.get("error")).isEqualTo("Invalid signature");
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void processWebhook_timestampJustInsideWindow_processesTransaction() throws Exception {
        // Given - timestamp 4 minutes old: inside the 5-minute window, must pass.
        String orderCode = "ENGABCDEF123456";
        String rawBody = baseJsonWithoutSignature(orderCode);
        String timestamp = String.valueOf(Instant.now().getEpochSecond() - 240);
        String signature = HmacUtils.hmacSha256Hex(webhookSecret, timestamp + "." + rawBody);

        User user = baseUser(17L);
        PaymentTransaction pending = pendingTx(orderCode, "MONTH", 17L);
        pending.setUser(user);

        when(paymentTransactionRepository.findByTransactionId("12345")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(
                rawBody, null, "sha256=" + signature, timestamp);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
    }

    @Test
    void processWebhook_invalidSignature_returnsInvalidSignature() throws Exception {
        // Given
        String orderCode = "ENGABCDEF123456";
        String baseJson = baseJsonWithoutSignature(orderCode);
        String rawBody = buildRawBodyWithSignature(baseJson, "wrong-signature");

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(rawBody);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(false);
        assertThat(actualResult.get("error")).isEqualTo("Invalid signature");
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void processWebhook_missingTransactionId_returnsMissingIdError() throws Exception {
        // Given
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", "ENGABCDEF123456");
        body.put("amount_in", "10000");
        String baseJson = objectMapper.writeValueAsString(body);
        String sig = computeExpectedSig(baseJson);
        String rawBody = baseJson.substring(0, baseJson.length() - 1) + ",\"signature\":\"" + sig + "\"}";

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(rawBody);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(false);
        assertThat(actualResult.get("error")).isEqualTo("Missing transaction id");
    }

    @Test
    void processWebhook_numericId_convertedToStringAndProcessed() throws Exception {
        // Given - id as number 999
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", 999);
        body.put("content", "ENGABCDEF123456");
        body.put("amount_in", "10000");
        body.put("gateway", "VCB");
        String baseJson = objectMapper.writeValueAsString(body);
        String sig = computeExpectedSig(baseJson);
        String rawBody = baseJson.substring(0, baseJson.length() - 1) + ",\"signature\":\"" + sig + "\"}";

        String orderCode = "ENGABCDEF123456";
        User user = baseUser(2L);
        PaymentTransaction pending = pendingTx(orderCode, "MONTH", 2L);
        pending.setUser(user);
        when(paymentTransactionRepository.findByTransactionId("999")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(rawBody);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getTransactionId()).isEqualTo("999");
    }

    @Test
    void processWebhook_transferAmountField_usedWhenAmountInMissing() throws Exception {
        // Given - uses transferAmount instead of amount_in
        String orderCode = "ENGABCDEF123456";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", 5555);
        body.put("content", "Thanh toan " + orderCode);
        body.put("transferAmount", 20000);
        body.put("gateway", "MB");
        String baseJson = objectMapper.writeValueAsString(body);
        String sig = computeExpectedSig(baseJson);
        String rawBody = baseJson.substring(0, baseJson.length() - 1) + ",\"signature\":\"" + sig + "\"}";

        User user = baseUser(3L);
        PaymentTransaction pending = pendingTx(orderCode, "YEAR", 3L);
        pending.setAmount(new BigDecimal("20000"));
        pending.setUser(user);

        when(paymentTransactionRepository.findByTransactionId("5555")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processWebhook(rawBody);

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(transactionCaptor.getValue().getGateway()).isEqualTo("MB");
    }

    // ---- processSePayTransaction ----

    @Test
    void processSePayTransaction_noOrderCodeInContent_returnsInvalidContent() {
        // Given
        String content = "random content without code";

        // When
        Map<String, Object> actualResult = paymentService.processSePayTransaction("tx1", content, new BigDecimal("10000"), "VCB", "{}");

        // Then
        assertThat(actualResult.get("success")).isEqualTo(false);
        assertThat(actualResult.get("error")).isEqualTo("Invalid content format");
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void processSePayTransaction_duplicateTransactionId_returnsSuccessWithoutChange() {
        // Given
        String orderCode = "ENGABCDEF123456";
        String content = "Pay " + orderCode;
        PaymentTransaction existing = pendingTx(orderCode, "MONTH", 1L);
        existing.setTransactionId("txDup");
        existing.setStatus("SUCCESS");
        when(paymentTransactionRepository.findByTransactionId("txDup")).thenReturn(Optional.of(existing));

        // When
        Map<String, Object> actualResult = paymentService.processSePayTransaction("txDup", content, new BigDecimal("10000"), "VCB", "{}");

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void processSePayTransaction_orderAlreadySuccess_returnsSuccessWithoutChange() {
        // Given
        String orderCode = "ENGABCDEF123456";
        String content = "Pay " + orderCode;
        when(paymentTransactionRepository.findByTransactionId("txNew")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(true);

        // When
        Map<String, Object> actualResult = paymentService.processSePayTransaction("txNew", content, new BigDecimal("10000"), "VCB", "{}");

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void processSePayTransaction_noPending_returnsError() {
        // Given
        String orderCode = "ENGABCDEF123456";
        String content = "Pay " + orderCode;
        when(paymentTransactionRepository.findByTransactionId("tx1")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.empty());

        // When
        Map<String, Object> actualResult = paymentService.processSePayTransaction("tx1", content, new BigDecimal("10000"), "VCB", "{}");

        // Then
        assertThat(actualResult.get("success")).isEqualTo(false);
        assertThat((String) actualResult.get("error")).contains("No pending order");
    }

    @Test
    void processSePayTransaction_yearPlan_calculatesExpiryPlusOneYear() {
        // Given
        String orderCode = "ENGYEAR12345678".substring(0, 15);
        orderCode = "ENGABCDEF123456";
        String content = "Thanh toan " + orderCode;
        User user = baseUser(10L);
        PaymentTransaction pending = pendingTx(orderCode, "YEAR", 10L);
        pending.setUser(user);
        when(paymentTransactionRepository.findByTransactionId("txY")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processSePayTransaction("txY", content, new BigDecimal("20000"), "VCB", "{}");

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getIsPremium()).isTrue();
        assertThat(savedUser.getPremiumExpiry()).isEqualTo(LocalDate.now().plusYears(1));
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getPremiumExpiry()).isEqualTo(LocalDate.now().plusYears(1));
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void processSePayTransaction_monthPlan_calculatesExpiryPlusOneMonth() {
        // Given
        String orderCode = "ENGABCDEF123456";
        String content = "Pay " + orderCode + " extra";
        User user = baseUser(11L);
        PaymentTransaction pending = pendingTx(orderCode, "MONTH", 11L);
        pending.setUser(user);
        when(paymentTransactionRepository.findByTransactionId("txM")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processSePayTransaction("txM", content, new BigDecimal("10000"), "VCB", "{}");

        // Then
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPremiumExpiry()).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    void processSePayTransaction_orderCodeExtractedCaseInsensitive() {
        // Given - content lower case
        String orderCodeUpper = "ENGABCDEF123456";
        String contentLower = "pay ENGABCDEF123456 please";
        User user = baseUser(12L);
        PaymentTransaction pending = pendingTx(orderCodeUpper, "MONTH", 12L);
        pending.setUser(user);
        when(paymentTransactionRepository.findByTransactionId("txCase")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCodeUpper, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCodeUpper, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.processSePayTransaction("txCase", contentLower, new BigDecimal("10000"), "VCB", "{}");

        // Then
        assertThat(actualResult.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository).save(any());
    }

    // ---- createOrder ----

    @Test
    void createOrder_monthPlan_createsPendingWithCorrectAmountAndQr() {
        // Given
        User user = baseUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.createOrder(1L, "MONTH");

        // Then
        assertThat(actualResult.get("planType")).isEqualTo("MONTH");
        assertThat(actualResult.get("amount")).isEqualTo(new BigDecimal("10000"));
        assertThat((String) actualResult.get("orderCode")).matches("ENG[A-Z0-9]{12}");
        assertThat((String) actualResult.get("qrUrl")).contains(bankAccount).contains(bankName);
        verify(paymentTransactionRepository).save(transactionCaptor.capture());
        PaymentTransaction saved = transactionCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getPlanType()).isEqualTo("MONTH");
        assertThat(saved.getUser()).isEqualTo(user);
    }

    @Test
    void createOrder_yearPlan_amountIs20000() {
        // Given
        User user = baseUser(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.createOrder(2L, "YEAR");

        // Then
        assertThat(actualResult.get("amount")).isEqualTo(new BigDecimal("20000"));
        assertThat(actualResult.get("planType")).isEqualTo("YEAR");
    }

    @Test
    void createOrder_userNotFound_throwsRuntimeException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When-Then
        try {
            paymentService.createOrder(999L, "MONTH");
            assertThat(false).as("should have thrown").isTrue();
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage()).contains("User not found");
        }
    }

    // ---- checkPendingPayments ----

    @Test
    void checkPendingPayments_noPending_doesNothing() {
        // Given
        when(paymentTransactionRepository.findTop10ByUserIdAndStatusOrderByIdDesc(1L, "PENDING")).thenReturn(List.of());

        // When
        paymentService.checkPendingPayments(1L);

        // Then
        verify(sePayApiService, never()).findTransactionByOrderCode(anyString(), any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void checkPendingPayments_findsMatchingTransaction_processesIt() {
        // Given
        String orderCode = "ENGABCDEF123456";
        User user = baseUser(1L);
        PaymentTransaction pending = pendingTx(orderCode, "MONTH", 1L);
        pending.setUser(user);
        pending.setAmount(new BigDecimal("10000"));
        when(paymentTransactionRepository.findTop10ByUserIdAndStatusOrderByIdDesc(1L, "PENDING")).thenReturn(List.of(pending));

        Map<String, Object> sePayTx = new HashMap<>();
        sePayTx.put("id", "sepay123");
        sePayTx.put("content", "Pay " + orderCode);
        sePayTx.put("transferAmount", "10000");
        sePayTx.put("gateway", "VCB");
        when(sePayApiService.findTransactionByOrderCode(orderCode, new BigDecimal("10000"))).thenReturn(Optional.of(sePayTx));
        // For processSePayTransaction inside polling, need mocks for idempotency checks
        when(paymentTransactionRepository.findByTransactionId("sepay123")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")).thenReturn(false);
        when(paymentTransactionRepository.findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING")).thenReturn(Optional.of(pending));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        paymentService.checkPendingPayments(1L);

        // Then
        verify(sePayApiService).findTransactionByOrderCode(orderCode, new BigDecimal("10000"));
        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
    }

    // ---- getPremiumStatus ----

    @Test
    void getPremiumStatus_premiumExpired_deactivatesAndReturnsNotPremium() {
        // Given
        User user = baseUser(1L);
        user.setIsPremium(true);
        user.setPremiumExpiry(LocalDate.now().minusDays(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user)).thenReturn(Optional.of(user));
        when(sePayApiService.isTokenConfigured()).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<String, Object> actualResult = paymentService.getPremiumStatus(1L);

        // Then
        assertThat(actualResult.get("isPremium")).isEqualTo(false);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getIsPremium()).isFalse();
    }

    @Test
    void getPremiumStatus_notPremium_pollsSePayAndRefreshes() {
        // Given
        User user = baseUser(5L);
        user.setIsPremium(false);
        User refreshed = baseUser(5L);
        refreshed.setIsPremium(true);
        refreshed.setPremiumExpiry(LocalDate.now().plusMonths(1));

        when(userRepository.findById(5L))
                .thenReturn(Optional.of(user))
                .thenReturn(Optional.of(refreshed));
        when(paymentTransactionRepository.findTop10ByUserIdAndStatusOrderByIdDesc(5L, "PENDING")).thenReturn(List.of());
        when(sePayApiService.isTokenConfigured()).thenReturn(true);

        // When
        Map<String, Object> actualResult = paymentService.getPremiumStatus(5L);

        // Then
        assertThat(actualResult.get("isPremium")).isEqualTo(true);
        verify(paymentTransactionRepository).findTop10ByUserIdAndStatusOrderByIdDesc(5L, "PENDING");
    }

    @Test
    void getPremiumStatus_premiumValid_returnsPremiumTrue() {
        // Given
        User user = baseUser(6L);
        user.setIsPremium(true);
        user.setPremiumExpiry(LocalDate.now().plusDays(10));
        when(userRepository.findById(6L)).thenReturn(Optional.of(user));
        when(sePayApiService.isTokenConfigured()).thenReturn(true);

        // When
        Map<String, Object> actualResult = paymentService.getPremiumStatus(6L);

        // Then
        assertThat(actualResult.get("isPremium")).isEqualTo(true);
        assertThat(actualResult.get("premiumExpiry")).isEqualTo(LocalDate.now().plusDays(10));
        assertThat(actualResult.get("pollingEnabled")).isEqualTo(true);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getPremiumStatus_notPremiumAndPollingDisabled_includesPollMessage() {
        // Given
        User user = baseUser(7L);
        user.setIsPremium(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user)).thenReturn(Optional.of(user));
        when(paymentTransactionRepository.findTop10ByUserIdAndStatusOrderByIdDesc(7L, "PENDING")).thenReturn(List.of());
        when(sePayApiService.isTokenConfigured()).thenReturn(false);

        // When
        Map<String, Object> actualResult = paymentService.getPremiumStatus(7L);

        // Then
        assertThat(actualResult.get("isPremium")).isEqualTo(false);
        assertThat(actualResult).containsKey("pollMessage");
        assertThat((String) actualResult.get("pollMessage")).contains("SEPAY_API_TOKEN");
    }
}
