package com.datn.engflow.service;

import com.datn.engflow.model.entity.PaymentTransaction;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.repository.PaymentTransactionRepository;
import com.datn.engflow.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
/**
 * class PaymentService.
 */
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final SePayApiService sePayApiService;

    @Value("${sepay.webhook.secret}")
    private String webhookSecret;

    @Value("${sepay.qr-url:https://qr.sepay.vn/img}")
    private String sepayQrUrl;

    @Value("${sepay.bank-account}")
    private String bankAccount;

    @Value("${sepay.bank-name}")
    private String bankName;

    @Transactional
    public Map<String, Object> processWebhook(String rawBody) {
        return processWebhook(rawBody, null, null, null);
    }

    @Transactional
    public Map<String, Object> processWebhook(String rawBody, String headerSignature) {
        return processWebhook(rawBody, headerSignature, null, null);
    }

    /**
     * Verify and process a SePay webhook notification.
     *
     * <p>SePay (production) signs the payload with HMAC-SHA256 over
     * {@code "<X-Sepay-Timestamp>.<rawBody>"} using the webhook secret, and sends
     * the digest in the {@code X-Sepay-Signature} header prefixed with
     * {@code "sha256="}. Legacy/simulated integrations may instead send the
     * signature in an {@code X-Signature} header or a {@code signature} body
     * field, computed over the body with the signature field stripped. All three
     * schemes are accepted; the first one that validates wins.</p>
     *
     * @param rawBody         the raw JSON request body
     * @param headerSignature legacy {@code X-Signature} header value (may be null)
     * @param sepaySignature  production {@code X-Sepay-Signature} header value (may be null)
     * @param sepayTimestamp  production {@code X-Sepay-Timestamp} header value (may be null)
     * @return result map with a {@code success} key
     */
    @Transactional
    public Map<String, Object> processWebhook(String rawBody, String headerSignature,
                                              String sepaySignature, String sepayTimestamp) {
        try {
            Map<String, Object> body = objectMapper.readValue(rawBody, new TypeReference<>() {});

            if (!isSignatureValid(rawBody, body, headerSignature, sepaySignature, sepayTimestamp)) {
                log.warn("Invalid HMAC signature from SePay webhook");
                return Map.of("success", false, "error", "Invalid signature");
            }

            // SePay sends "id" as a JSON number; convert defensively to avoid
            // ClassCastException on (String) casts for numeric ids.
            Object idObj = body.get("id");
            String transactionId = idObj != null ? String.valueOf(idObj) : null;
            if (transactionId == null || transactionId.isBlank() || "null".equals(transactionId)) {
                log.warn("Webhook missing transaction id");
                return Map.of("success", false, "error", "Missing transaction id");
            }

            String content = (String) body.getOrDefault("content", "");
            // SePay webhooks carry "transferAmount"; keep "amount_in" for
            // legacy/simulated payloads. Fallback to "amount" if needed.
            Object amountRaw = body.containsKey("amount_in") ? body.get("amount_in") :
                               (body.containsKey("transferAmount") ? body.get("transferAmount") : body.getOrDefault("amount", "0"));
            BigDecimal amount = new BigDecimal(String.valueOf(amountRaw));
            String gateway = (String) body.getOrDefault("gateway", "");

            return processSePayTransaction(transactionId, content, amount, gateway, rawBody);
        } catch (Exception e) {
            log.error("SePay webhook processing failed", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Core payment-processing logic shared by the webhook handler and the
     * API-polling fallback. Updates an existing PENDING row to SUCCESS and
     * activates the user's premium subscription. Idempotent: if the transaction
     * or order has already been fulfilled, it returns success without changes.
     *
     * @param transactionId the SePay transaction id (numeric, as string)
     * @param content       the bank transfer content field (must contain orderCode token)
     * @param amount        the transfer amount
     * @param gateway       the bank gateway name
     * @param rawBody       the raw webhook payload (stored for audit)
     * @return result map with "success" key
     */
    @Transactional
    public Map<String, Object> processSePayTransaction(String transactionId, String content,
                                                       BigDecimal amount, String gateway, String rawBody) {
        // Extract orderCode token (ENGXXXXXXXXXXXX, 15 chars) from arbitrary bank content.
        // The token has NO separator: the VietQR generator strips "_" from the des=
        // parameter when embedding it into EMVCo field 62/08 (verified by decoding
        // generated QR payloads), so the code must survive bank-side normalization.
        // The underscore is optional in the pattern so legacy "ENG_" PENDING rows
        // created before the format change can still be fulfilled.
        String orderCode = null;
        if (content != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("ENG_?[A-Z0-9]{12}").matcher(content.toUpperCase());
            if (m.find()) {
                orderCode = m.group();
            }
        }

        if (orderCode == null) {
            log.warn("No valid orderCode in transaction content: {}", content);
            return Map.of("success", false, "error", "Invalid content format");
        }

        // Idempotency: skip if this transaction was already recorded,
        // or if the order has already been fulfilled.
        Optional<PaymentTransaction> existing = paymentTransactionRepository.findByTransactionId(transactionId);
        if (existing.isPresent()
                || paymentTransactionRepository.existsByOrderCodeAndStatus(orderCode, "SUCCESS")) {
            log.info("Duplicate webhook/polling, already processed: {} / {}", orderCode, transactionId);
            return Map.of("success", true);
        }

        // Resolve user/plan from the newest PENDING row for this order.
        Optional<PaymentTransaction> pendingOpt = paymentTransactionRepository
                .findFirstByOrderCodeAndStatusOrderByIdDesc(orderCode, "PENDING");
        if (pendingOpt.isEmpty()) {
            // Legacy fallback: rows created before the format change store "ENG_" +
            // 12 chars, while QR-originated content arrives underscore-free. Try the
            // underscore variant so in-flight orders are not orphaned.
            String legacyCode = orderCode.startsWith("ENG_") ? orderCode : "ENG_" + orderCode.substring(3);
            pendingOpt = paymentTransactionRepository
                    .findFirstByOrderCodeAndStatusOrderByIdDesc(legacyCode, "PENDING");
        }
        if (pendingOpt.isEmpty()) {
            log.warn("No pending order found for: {}", orderCode);
            return Map.of("success", false, "error", "No pending order for: " + orderCode);
        }

        PaymentTransaction pending = pendingOpt.get();
        User user = pending.getUser();
        String planType = pending.getPlanType();
        LocalDate premiumExpiry = calculateExpiry(planType);

        // Update user premium
        user.setIsPremium(true);
        user.setPremiumExpiry(premiumExpiry);
        userRepository.save(user);

        // Mark the existing pending row as fulfilled instead of inserting a
        // duplicate row with the same order_code.
        pending.setTransactionId(transactionId);
        pending.setAmount(amount);
        pending.setGateway(gateway);
        pending.setContent(content);
        pending.setStatus("SUCCESS");
        pending.setPremiumExpiry(premiumExpiry);
        pending.setWebhookRaw(rawBody);
        paymentTransactionRepository.save(pending);

        log.info("Premium activated for user {}: {} until {}", user.getId(), planType, premiumExpiry);
        return Map.of("success", true);
    }

    /**
     * Fallback polling mechanism: when the SePay webhook callback cannot reach
     * the local server (e.g. localhost / Docker development), this method
     * queries the SePay API directly to check if any pending orders have
     * been paid. Called from getPremiumStatus before returning status.
     *
     * @param userId the authenticated user id
     */
    @Transactional
    public void checkPendingPayments(Long userId) {
        List<PaymentTransaction> pendingTxs = paymentTransactionRepository
                .findTop10ByUserIdAndStatusOrderByIdDesc(userId, "PENDING");

        if (pendingTxs == null || pendingTxs.isEmpty()) {
            return;
        }

        log.debug("Polling SePay API for {} pending order(s) for user {}", pendingTxs.size(), userId);

        for (PaymentTransaction tx : pendingTxs) {
            Optional<Map<String, Object>> sePayTx = sePayApiService
                    .findTransactionByOrderCode(tx.getOrderCode(), tx.getAmount());
            if (sePayTx.isPresent()) {
                Map<String, Object> stx = sePayTx.get();
                String stxId = stx.get("id") != null ? String.valueOf(stx.get("id")) : null;
                String content = (String) stx.getOrDefault("content", tx.getContent());
                BigDecimal amount = new BigDecimal(String.valueOf(stx.getOrDefault("transferAmount", tx.getAmount())));
                String gateway = (String) stx.getOrDefault("gateway", tx.getGateway());
                String rawBody;
                try {
                    rawBody = objectMapper.writeValueAsString(stx);
                } catch (Exception ex) {
                    rawBody = "{}";
                }

                log.info("SePay API polling detected payment for order: {}", tx.getOrderCode());
                processSePayTransaction(stxId, content, amount, gateway, rawBody);
            }
        }
    }

    @Transactional
    public Map<String, Object> createOrder(Long userId, String planType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String orderCode = "ENG" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        BigDecimal amount = "YEAR".equals(planType) ? new BigDecimal("20000") : new BigDecimal("10000");

        PaymentTransaction pendingTx = PaymentTransaction.builder()
                .user(user)
                .orderCode(orderCode)
                .amount(amount)
                .planType(planType)
                .status("PENDING")
                .build();
        paymentTransactionRepository.save(pendingTx);

        // SePay QR image API: the transfer-content parameter is `des` (NOT `content`).
        // Verified against official docs and by decoding the generated QR payload:
        // `content=` is silently ignored (EMVCo field 62 absent), `des=` is embedded
        // so banking apps auto-fill the transfer content when the QR is scanned.
        // The QR generator strips "_" from `des`, so orderCode is underscore-free.
        // Source: https://docs.sepay.vn/tao-qr-code-vietqr-dong.html
        String qrUrl = sepayQrUrl + "?acc=" + bankAccount + "&amount=" + amount.longValue()
                + "&des=" + orderCode + "&bank=" + bankName;

        return Map.of(
                "orderCode", orderCode,
                "amount", amount,
                "qrUrl", qrUrl,
                "planType", planType
        );
    }

    @Transactional
    public Map<String, Object> getPremiumStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fallback: poll SePay API for any pending payments that were
        // completed but whose webhook never reached localhost.
        if (!Boolean.TRUE.equals(user.getIsPremium())) {
            checkPendingPayments(userId);
            // Refresh user entity after potential DB updates
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        boolean isPremium = Boolean.TRUE.equals(user.getIsPremium());
        LocalDate expiry = user.getPremiumExpiry();

        if (isPremium && expiry != null && expiry.isBefore(LocalDate.now())) {
            isPremium = false;
            user.setIsPremium(false);
            userRepository.save(user);
        }

        HashMap<String, Object> result = new HashMap<>();
        result.put("isPremium", isPremium);
        result.put("premiumExpiry", expiry);

        // Diagnostic: let frontend know whether fallback polling is active
        boolean pollingEnabled = sePayApiService.isTokenConfigured();
        result.put("pollingEnabled", pollingEnabled);
        if (!isPremium && !pollingEnabled) {
            result.put("pollMessage",
                "SePay API token chua cau hinh. Polling fallback bi vo hieu. " +
                "Admin can set SEPAY_API_TOKEN va rebuild Docker.");
        }

        return result;
    }

    /**
     * Maximum allowed clock skew between the X-Sepay-Timestamp header and
     * server time before a webhook is rejected as a replay.
     * Source: https://developer.sepay.vn/vi/sepay-webhooks/xac-thuc —
     * "if (abs(time() - $timestamp) > 300)" (5 minutes).
     */
    private static final long REPLAY_WINDOW_MS = 5 * 60 * 1000L;

    /**
     * Validate the webhook signature against any supported scheme.
     *
     * <p>The production scheme additionally enforces the replay window: a
     * request whose X-Sepay-Timestamp deviates more than {@link #REPLAY_WINDOW_MS}
     * from server time is rejected even if the signature itself is valid, so a
     * captured payload cannot be replayed indefinitely. The legacy/simulated
     * schemes carry no timestamp header (they are exercised by tests and
     * manual simulation) and therefore skip the replay check.</p>
     *
     * @param rawBody         raw JSON request body
     * @param body            parsed body (used for the legacy body-field signature)
     * @param headerSignature legacy X-Signature header value (may be null)
     * @param sepaySignature  production X-Sepay-Signature header value (may be null)
     * @param sepayTimestamp  production X-Sepay-Timestamp header value (may be null)
     * @return true if at least one scheme validates
     */
    private boolean isSignatureValid(String rawBody, Map<String, Object> body,
                                     String headerSignature, String sepaySignature,
                                     String sepayTimestamp) {
        // Production SePay: X-Sepay-Signature: sha256=<hex>, signed over "<ts>.<body>"
        if (sepaySignature != null && !sepaySignature.isBlank()
                && sepayTimestamp != null && !sepayTimestamp.isBlank()) {
            long timestamp;
            try {
                timestamp = Long.parseLong(sepayTimestamp.trim());
            } catch (NumberFormatException e) {
                log.warn("Webhook replay check failed: non-numeric X-Sepay-Timestamp");
                return false;
            }
            long skew = Math.abs(System.currentTimeMillis() - timestamp * 1000L);
            if (skew > REPLAY_WINDOW_MS) {
                log.warn("Webhook replay rejected: timestamp skew {}ms exceeds {}ms window", skew, REPLAY_WINDOW_MS);
                return false;
            }
            String received = sepaySignature.startsWith("sha256=")
                    ? sepaySignature.substring("sha256=".length())
                    : sepaySignature;
            String expected = HmacUtils.hmacSha256Hex(webhookSecret, sepayTimestamp.trim() + "." + rawBody);
            if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    received.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }

        // Legacy/simulated: signature over the body with the signature field stripped,
        // sent as X-Signature header or a "signature" body field.
        String expectedSig = HmacUtils.hmacSha256Hex(webhookSecret, stripSignature(rawBody));
        String receivedSig = headerSignature != null && !headerSignature.isBlank()
                ? headerSignature
                : pickSignature(body);
        return MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8),
                receivedSig.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * SePay signs the payload without the "signature" field itself.
     * Strips the signature field(s) from the raw JSON before computing HMAC.
     * Uses raw-string removal to preserve original key order, falls back to Map re-serialize.
     */
    private String stripSignature(String rawBody) {
        try {
            // Attempt raw-string removal to keep original ordering and whitespace handling minimal
            String stripped = rawBody.replaceAll(",\\s*\"[Ss]ignature\"\\s*:\\s*\"[^\"]*\"\\s*", "");
            stripped = stripped.replaceAll("\"[Ss]ignature\"\\s*:\\s*\"[^\"]*\"\\s*,?\\s*", "");
            // Clean up artifacts: double commas, trailing comma before }
            stripped = stripped.replaceAll(",\\s*,", ",");
            stripped = stripped.replaceAll(",\\s*}", "}");
            stripped = stripped.replaceAll("\\{\\s*,", "{");
            if (!stripped.contains("\"signature\"") && !stripped.contains("\"Signature\"")) {
                return stripped;
            }
            // Fallback: Map-based stripping preserves logical content if raw regex missed (e.g., numeric signature)
            Map<String, Object> body = objectMapper.readValue(rawBody, new TypeReference<>() {});
            body.remove("signature");
            body.remove("Signature");
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.warn("Could not strip signature from body, using raw body");
            return rawBody;
        }
    }

    /**
     * Extracts the received signature from the body. The header-based
     * signature (X-Signature) is passed by the controller.
     */
    private String pickSignature(Map<String, Object> body) {
        Object sig = body.get("signature");
        if (sig == null) sig = body.get("Signature");
        return sig != null ? String.valueOf(sig) : "";
    }

    private LocalDate calculateExpiry(String planType) {
        LocalDate now = LocalDate.now();
        if ("YEAR".equals(planType)) return now.plusYears(1);
        return now.plusMonths(1);
    }
}



