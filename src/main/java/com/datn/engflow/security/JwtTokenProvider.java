package com.datn.engflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Ký và xác minh access token HS256 cho toàn bộ API.
 *
 * <p>Tầng bảo mật: đây là nơi duy nhất chạm vào bí mật {@code jwt.secret} và đặt
 * hạn dùng cho token. Token được sinh ở luồng đăng nhập rồi mang trong header
 * {@code Authorization: Bearer}; {@link JwtAuthenticationFilter} gọi
 * {@link #validateToken(String)} và {@link #getEmailFromJWT(String)} ở mọi request
 * để dựng principal, sau đó {@link CustomUserDetailsService} nạp {@link UserPrincipal}
 * tương ứng.
 *
 * <p>Hệ thống không có refresh token: {@code jwt.expiration} mặc định 900000 ms
 * (15 phút) nên hết hạn buộc người dùng đăng nhập lại.
 *
 * <p>Chỉ claim {@code role} và {@code isPremium} được nhúng vào token; quyền admin
 * thực tế vẫn được đọc lại từ DB qua {@link UserPrincipal}, nên tắt quyền admin có
 * hiệu lực ngay ở request kế tiếp dù token còn hạn.
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    /** HMAC key dựng từ {@code jwt.secret}; {@code Keys.hmacShaKeyFor} tự chọn độ dài key hợp lệ. */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Đóng gói một access token đã ký cho đúng một principal.
     *
     * <p>Claim {@code isPremium} được ghi {@code false} khi truyền {@code null}, để
     * token không bao giờ mang giá trị null mà {@link JwtAuthenticationFilter} hay service
     * phải xử lý lại.
     *
     * @param email email đăng nhập, đóng vai trò {@code sub} và cũng là khoá tra user
     * @param role tên role sẽ nhúng vào claim {@code role}
     * @param isPremium cờ premium tại thời điểm đăng nhập; null bị coi là false
     * @return token HS256 đã ký, hạn sau {@code jwt.expiration} mili-giây kể từ lúc gọi
     */
    public String generateToken(String email, String role, Boolean isPremium) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("isPremium", isPremium != null && isPremium)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Đọc email (claim {@code sub}) ra khỏi token đã ký.
     *
     * @param token access token lấy từ header {@code Authorization}
     * @return giá trị claim {@code sub}, tức email dùng để tra user trong DB
     * @throws io.jsonwebtoken.security.SignatureException nếu chữ ký không khớp
     * @throws io.jsonwebtoken.ExpiredJwtException nếu token đã hết hạn
     * @throws io.jsonwebtoken.MalformedJwtException nếu token không phải JWT hợp lệ
     */
    public String getEmailFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Kiểm tra chữ ký và hạn của token.
     *
     * <p>Hết hạn không phải lỗi im lặng: {@link ExpiredJwtException} được ném ra có chủ
     * đích để {@link JwtAuthenticationFilter} trả 401 với thông báo "đăng nhập lại" thay vì
     * để request đi tiếp không có principal (kết quả là 403 gây hiểu nhầm).
     *
     * @param authToken access token lấy từ header {@code Authorization}
     * @return {@code true} khi chữ ký đúng và token còn hạn
     * @throws ExpiredJwtException nếu token hợp lệ về chữ ký nhưng đã quá hạn
     * @throws io.jsonwebtoken.JwtException nếu token hỏng hoặc chữ ký sai
     */
    public boolean validateToken(String authToken) throws ExpiredJwtException {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (ExpiredJwtException ex) {
            throw ex;
        }
    }
}
