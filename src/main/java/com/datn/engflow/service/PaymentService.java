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
        return processWebhook(rawBody, null);
    }

    @Transactional
    public Map<String, Object> processWebhook(String rawBody, String headerSignature) {
        try {
            Map<String, Object> body = objectMapper.readValue(rawBody, new TypeReference<>() {});

            // Verify HMAC-SHA256 over the body with the signature field removed.
            // SePay signs the payload and sends the signature either in the
            // request body ("signature") or as a header (X-Signature / x-signature).
            String expectedSig = HmacUtils.hmacSha256Hex(webhookSecret, stripSignature(rawBody));
            String receivedSig = headerSignature != null && !headerSignature.isBlank()
                    ? headerSignature
                    : pickSignature(body);

            if (!expectedSig.equals(receivedSig)) {
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
        // Extract orderCode token (ENG_XXXXXXXXXXXX) from arbitrary bank content.
        String orderCode = null;
        if (content != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("ENG_[A-Z0-9]{12}").matcher(content.toUpperCase());
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
                .findTop5ByUserIdAndStatusOrderByIdDesc(userId, "PENDING");

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

        String orderCode = "ENG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        BigDecimal amount = "YEAR".equals(planType) ? new BigDecimal("20000") : new BigDecimal("10000");

        PaymentTransaction pendingTx = PaymentTransaction.builder()
                .user(user)
                .orderCode(orderCode)
                .amount(amount)
                .planType(planType)
                .status("PENDING")
                .build();
        paymentTransactionRepository.save(pendingTx);

        String qrContent = bankAccount + "|" + orderCode + "|" + amount;
        String qrUrl = sepayQrUrl + "?acc=" + bankAccount + "&amount=" + amount.longValue()
                + "&content=" + orderCode + "&bank=" + bankName;

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



