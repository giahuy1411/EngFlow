package com.datn.engflow.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Ticket HMAC-SHA256 cho các object media riêng tư trên proxy MinIO
 * ({@code /api/v1/media/**}) — audit-v7 F55.
 *
 * <p>Bản ghi âm speaking/shadowing của người học là dữ liệu cá nhân. Trước bản vá, proxy phục vụ
 * BẤT KỲ object key nào cho BẤT KỲ ai gọi — tức IDOR ngay khi key lộ (log, referrer, shoulder-surfing).
 * Vì {@code <audio src>} của trình duyệt không gắn được header {@code Authorization}, phải dùng URL
 * có chữ ký thay vì JWT-in-query; và cũng KHÔNG nhét JWT vào query vì URL sẽ rò vào access log/lịch
 * sử — chữ ký dưới đây chỉ mở đúng một object trong thời hạn ngắn nên an toàn hơn.</p>
 *
 * <p>URL được sinh theo từng response dạng {@code ...?exp=<epoch-sec>&sig=<hex>} với
 * {@code sig = HMAC(secret, objectKey + "." + exp)}. Secret dùng chung với {@code jwt.secret}.</p>
 */
@Slf4j
@Component
public class MediaSigner {

    static final long TTL_SECONDS = 6 * 3600;

    private final String secret;

    public MediaSigner(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    /** Trả chuỗi {@code "exp=<epoch>&sig=<hex>"} để nối sau dấu {@code "?"}. */
    public String paramsForObject(String objectKey) {
        long exp = System.currentTimeMillis() / 1000L + TTL_SECONDS;
        return "exp=" + exp + "&sig=" + hmac(objectKey, exp);
    }

    /**
     * Kiểm chứng bộ ba (objectKey, exp, sig) theo thời gian hằng số.
     *
     * <p>Hai điều kiện, thiếu một là từ chối: chữ ký khớp và {@code exp} chưa qua. So sánh chữ ký
     * bằng {@link MessageDigest#isEqual} (constant-time) để không rò thông tin qua timing. Chuỗi
     * rỗng/null trả {@code false} ngay; {@code exp} không parse được cũng vậy.</p>
     */
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
            // HmacSHA256 là thuật toán bắt buộc phải có ở mọi JDK; nhánh này trên thực tế không tới được.
            throw new IllegalStateException("Media signing unavailable", e);
        }
    }
}
