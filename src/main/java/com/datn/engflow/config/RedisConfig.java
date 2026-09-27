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

@Configuration
@EnableCaching
/**
 * class RedisConfig.
 */
public class RedisConfig {

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
