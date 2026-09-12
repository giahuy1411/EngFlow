package com.datn.engflow.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * audit-v7 F55: HMAC-SHA256 "ticket" for private media objects on the MinIO
 * proxy ({@code /api/v1/media/**}). Learner speaking/shadowing recordings are
 * personal data; the proxy used to serve ANY object key to ANY caller, which
 * is an IDOR once a key leaks (logs, referrer, shoulder-surfing).
 *
 * <p>URLs are generated per-response as {@code ...?exp=<epoch-sec>&sig=<hex>}
 * where {@code sig = HMAC(secret, objectKey + "." + exp)}. The browser cannot
 * attach an Authorization header to {@code <audio src>}, hence signed URLs
 * rather than JWT-in-query (a JWT in a URL leaks to access logs/history —
 * this deliberately avoids that).
 */
@Slf4j
@Component
public class MediaSigner {

    static final long TTL_SECONDS = 6 * 3600;

    private final String secret;

    public MediaSigner(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    /** Returns "exp=<epoch>&sig=<hex>" to append after "?". */
    public String paramsForObject(String objectKey) {
        long exp = System.currentTimeMillis() / 1000L + TTL_SECONDS;
        return "exp=" + exp + "&sig=" + hmac(objectKey, exp);
    }

    /** Constant-time verification of the (objectKey, exp, sig) triple. */
    public boolean verify(String objectKey, String expRaw, String sig) {
        if (objectKey == null || expRaw == null || sig == null || sig.isBlank()) {
            return false;
        }
        long exp;
        try {
            exp = Long.parseLong(expRaw.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (exp < System.currentTimeMillis() / 1000L) {
            return false;
        }
        byte[] expected = hmac(objectKey, exp).getBytes(StandardCharsets.UTF_8);
        byte[] received = sig.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, received);
    }

    private String hmac(String objectKey, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal((objectKey + "." + exp).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            // HMAC-SHA256 is mandatory in every JDK; this path is unreachable in practice.
            throw new IllegalStateException("Media signing unavailable", e);
        }
    }
}
