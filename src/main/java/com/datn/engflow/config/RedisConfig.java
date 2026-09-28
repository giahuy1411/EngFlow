package com.datn.engflow.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Wiring Redis cho cả truy cập key trực tiếp lẫn cache dựa trên annotation của
 * Spring.
 *
 * <p>Ở đây có hai lớp chồng nhau. {@link RedisTemplate} được dùng trực tiếp cho
 * counter rate-limit, marker streak và session game — key lưu dạng chuỗi thuần để
 * còn soi được bằng {@code redis-cli}. {@link CacheManager} đỡ các lookup
 * {@code @Cacheable} — chủ yếu là proxy từ điển trong
 * {@link com.datn.engflow.service.DictionaryService} — và mang một ngoại lệ TTL
 * riêng cho từng cache.</p>
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Tạo template dùng cho đọc/ghi Redis trực tiếp.
     *
     * <p>Key lưu dạng chuỗi thuần; value và hash value dùng
     * {@link GenericJackson2JsonRedisSerializer}, serializer này nhúng type hint
     * nên value đa hình round-trip được. Đổi sang serializer không nhúng type sẽ
     * làm {@code RedisTemplate<String, Object>} không biết dựng lại class nào.</p>
     *
     * @param connectionFactory factory gắn với Redis server đã cấu hình
     * @return template đã cấu hình
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /** TTL mặc định (giờ) áp cho mọi cache, trừ các cache có override riêng. */
    @org.springframework.beans.factory.annotation.Value("${cache.ttl-hours:1}")
    private long cacheTtlHours;

    /** audit-v17 L2: nhớ trong bao lâu một 404 đã xác nhận ("từ này không tồn tại"). */
    @org.springframework.beans.factory.annotation.Value("${dictionary.miss-ttl-minutes:30}")
    private long dictionaryMissTtlMinutes;

    /**
     * Tạo cache manager đỡ các lookup {@code @Cacheable}.
     *
     * <p>Mọi cache dùng chung {@code cache.ttl-hours} (mặc định 1h), TRỪ cache
     * {@code dictionaryMiss} dùng {@code dictionary.miss-ttl-minutes} ngắn hơn
     * (mặc định 30m), để một từ đã bị upstream xác nhận là không có sẽ được kiểm
     * tra lại sớm hơn so với một kết quả thành công.</p>
     *
     * @param connectionFactory factory gắn với Redis server đã cấu hình
     * @return manager với TTL mặc định dùng chung và một override theo cache
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(cacheTtlHours))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Vòng audit-v17 remove-limits (L2): một 404 đã xác nhận nhận TTL NGẮN để từ gõ sai
        // không bị gọi lại upstream chậm trong suốt một giờ, mà một từ thật xuất hiện sau đó
        // vẫn tìm thấy được trong vòng 30 phút. Mọi cache khác giữ nguyên default dùng chung.
        RedisCacheConfiguration missConfig = config.entryTtl(Duration.ofMinutes(dictionaryMissTtlMinutes));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(java.util.Map.of("dictionaryMiss", missConfig))
                .build();
    }
}
