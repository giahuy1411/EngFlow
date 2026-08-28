package com.datn.engflow.controller.payment;

import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
/**
 * class PaymentController.
 */
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/api/webhook/sepay")
    public ResponseEntity<Map<String, Object>> sepayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Signature", required = false) String headerSignature) {
        Map<String, Object> result = paymentService.processWebhook(rawBody, headerSignature);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/v1/payment/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> body) {
        String planType = body.getOrDefault("planType", "MONTH");
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> result = paymentService.createOrder(userPrincipal.getId(), planType);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/v1/payment/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> status = paymentService.getPremiumStatus(userPrincipal.getId());
        return ResponseEntity.ok(status);
    }
}
