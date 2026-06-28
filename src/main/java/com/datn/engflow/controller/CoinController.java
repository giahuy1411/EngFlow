package com.datn.engflow.controller;

import com.datn.engflow.security.UserPrincipal;
import com.datn.engflow.service.CoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CoinController {

    private final CoinService coinService;

    @GetMapping("/coins/balance")
    public ResponseEntity<?> getBalance(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("coins", coinService.getBalance(userPrincipal.getId())));
    }

    @PostMapping("/coins/earn")
    public ResponseEntity<?> earnCoins(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, Integer> payload) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Integer amountObj = payload.get("amount");
        if (amountObj == null || amountObj <= 0 || amountObj > 10000) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount phải từ 1 đến 10000"));
        }
        coinService.earnCoins(userPrincipal.getId(), amountObj);
        return ResponseEntity.ok(Map.of("message", "Coins earned successfully"));
    }
}
