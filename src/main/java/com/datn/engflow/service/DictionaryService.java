package com.datn.engflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Tra từ điển qua dictionaryapi.dev với Redis cache.
 *
 * <p>audit-v17 remove-limits round (L2) — negative caching. The previous version used
 * {@code @Cacheable(value = "dictionary", unless = "#result == '[]'")}. That annotation collapsed
 * TWO different causes into the same {@code "[]"} token and then cached NEITHER:
 * <ul>
 *   <li>a genuine upstream 404 — the word really does not exist;</li>
 *   <li>an upstream failure — timeout / connect error / 5xx (measured: the upstream's own TTFB is
 *       ~19.5 s, and it returns HTTP 522 under load).</li>
 * </ul>
 * So every repeat lookup of a MISSING word re-paid the full ~20 s. The two causes are now separated
 * explicitly, using two caches with independent TTLs:
 * <ul>
 *   <li>{@code dictionary} — a real payload, TTL {@code cache.ttl-hours} (1 h);</li>
 *   <li>{@code dictionaryMiss} — a confirmed 404, TTL {@code dictionary.miss-ttl-minutes} (30 min).</li>
 * </ul>
 * A FAILURE is written to neither cache: it is transient, and caching it would turn a momentary
 * upstream hiccup into a half-hour of "word does not exist".
 *
 * <p>Kept as a separate service (not inlined into the controller) because the cache is accessed
 * through the injected {@link CacheManager} — the same Redis-backed manager the rest of the app uses.
 */
@Service
@Slf4j
public class DictionaryService {

    /** Real payloads. TTL comes from the cache manager (cache.ttl-hours, default 1 h). */
    static final String CACHE_HIT = "dictionary";
    /** Confirmed 404s. TTL comes from the cache manager (dictionary.miss-ttl-minutes, default 30 min). */
    static final String CACHE_MISS = "dictionaryMiss";

    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;
    private final String upstreamTemplate;

    /**
     * Single constructor — Spring must be able to autowire it unambiguously. (A second convenience
     * constructor would leave Spring with no way to choose one and fail context startup with
     * "No default constructor found"; callers/tests pass the upstream base explicitly instead.)
     */
    public DictionaryService(RestTemplate restTemplate, CacheManager cacheManager,
                             @org.springframework.beans.factory.annotation.Value(
                                     "${dictionary.upstream-url:https://api.dictionaryapi.dev/api/v2/entries/en}")
                             String upstreamBase) {
        this.restTemplate = restTemplate;
        this.cacheManager = cacheManager;
        // Configurable so a self-hosted dictionary (or a test stub) can be pointed at without a code
        // change; the default is the public upstream, so behaviour is unchanged until the env is set.
        this.upstreamTemplate = upstreamBase.replaceAll("/+$", "") + "/{w}";
    }

    /**
     * Look up {@code clean} (already sanitised + lower-cased) and return the raw upstream JSON, or
     * {@code "[]"} when the word does not exist. The return value is unchanged from before — the
     * controller still answers HTTP 200 with {@code "[]"} — but a confirmed miss is now remembered.
     *
     * <p><b>Fail-soft is total (cross-review round).</b> Redis is a cache, not the source of truth:
     * a cache READ failure must fall through to the upstream, and a cache WRITE failure must not
     * change the answer. Both are isolated below. Concretely, the earlier shape put
     * {@code hit.put(...)} inside the same {@code try} as the upstream call, so a Redis write error
     * was swallowed by the generic catch and turned a FETCHED word into {@code "[]"} — the user was
     * told "not found" for a word that exists.
     */
    public String lookup(String clean) {
        // 1) Cache reads — fail-soft: a Redis problem must not 500 the endpoint (the controller has
        //    no handler for it, and the class is documented fail-soft).
        try {
            Cache miss = cacheManager.getCache(CACHE_MISS);
            if (miss != null && miss.get(clean) != null) {
                return "[]";
            }
            Cache hit = cacheManager.getCache(CACHE_HIT);
            if (hit != null) {
                Cache.ValueWrapper cached = hit.get(clean);
                if (cached != null) {
                    Object value = cached.get();
                    return value == null ? "[]" : value.toString();
                }
            }
        } catch (Exception cacheReadErr) {
            log.warn("Dictionary cache read failed for '{}': {}", clean, cacheReadErr.getMessage());
            // fall through to the upstream
        }

        // 2) Upstream. Only THIS block decides the answer.
        String body;
        try {
            body = restTemplate.getForObject(upstreamTemplate, String.class, clean);
        } catch (HttpClientErrorException.NotFound e) {
            // The word genuinely does not exist -> remember it (short TTL) so the next lookup is instant.
            putQuietly(CACHE_MISS, clean, "[]");
            return "[]";
        } catch (Exception e) {
            // Timeout / connect error / 5xx — TRANSIENT. Do NOT cache; the word may exist.
            log.warn("Dictionary proxy failed for '{}': {}", clean, e.getMessage());
            return "[]";
        }
        if (body == null) {
            // No body is ambiguous — treat as a transient failure, do not cache.
            return "[]";
        }
        // 3) Cache the real payload. A write failure is logged but MUST NOT alter the answer.
        putQuietly(CACHE_HIT, clean, body);
        return body;
    }

    /** Best-effort cache write: a failure here never changes the answer the caller receives. */
    private void putQuietly(String cacheName, String key, String value) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.put(key, value);
            }
        } catch (Exception e) {
            log.warn("Dictionary cache write failed for '{}': {}", key, e.getMessage());
        }
    }
}
