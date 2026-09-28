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
 * Redis wiring for both direct key access and Spring's annotation-based cache.
 *
 * <p>Two overlapping layers live here. {@link RedisTemplate} is used directly for
 * rate-limit counters, streak markers and game sessions, keyed as plain strings
 * so they are inspectable with {@code redis-cli}. The {@link CacheManager} backs
 * {@code @Cacheable} lookups — chiefly the dictionary proxy in
 * {@link com.datn.engflow.service.DictionaryService} — and carries a per-cache
 * TTL exception.</p>
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Creates the template used for direct Redis reads and writes.
     *
     * <p>Keys are stored as plain strings; values and hash values use
     * {@link GenericJackson2JsonRedisSerializer}, which embeds type hints so
     * polymorphic values round-trip.</p>
     *
     * @param connectionFactory factory bound to the configured Redis server
     * @return the configured template
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

    @org.springframework.beans.factory.annotation.Value("${cache.ttl-hours:1}")
    private long cacheTtlHours;

    /** audit-v17 L2: how long a confirmed 404 ("word does not exist") is remembered. */
    @org.springframework.beans.factory.annotation.Value("${dictionary.miss-ttl-minutes:30}")
    private long dictionaryMissTtlMinutes;

    /**
     * Creates the cache manager backing {@code @Cacheable} lookups.
     *
     * <p>All caches share {@code cache.ttl-hours} (default 1h) except the
     * {@code dictionaryMiss} cache, which uses the shorter
     * {@code dictionary.miss-ttl-minutes} (default 30m) so a word confirmed
     * absent by the upstream is re-checked sooner than a successful result.</p>
     *
     * @param connectionFactory factory bound to the configured Redis server
     * @return a manager with a shared default TTL and a per-cache override
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(cacheTtlHours))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // audit-v17 remove-limits round (L2): a confirmed 404 gets a SHORT TTL so a typo'd word is
        // not re-fetched from the slow upstream for an hour, while a real word that appears later
        // still becomes findable within 30 minutes. Every other cache keeps the shared default.
        RedisCacheConfiguration missConfig = config.entryTtl(Duration.ofMinutes(dictionaryMissTtlMinutes));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(java.util.Map.of("dictionaryMiss", missConfig))
                .build();
    }
}
