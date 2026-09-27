package com.datn.engflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * audit-v17 remove-limits round (L2) — negative caching.
 *
 * <p>The old {@code @Cacheable(unless = "#result == '[]'")} collapsed a genuine 404 and a transient
 * upstream failure into the same {@code "[]"} token and cached neither, so every repeat lookup of a
 * missing word re-paid the ~20 s upstream. These tests pin the new behaviour:
 * <ul>
 *   <li>a 404 is remembered (a second lookup does NOT touch the network);</li>
 *   <li>a transient failure (timeout/5xx) is NOT cached (the word may exist — a later call retries);</li>
 *   <li>a real payload is cached and served without a second call.</li>
 * </ul>
 *
 * <p>Uses {@link ConcurrentMapCacheManager} so no Redis is required; the per-cache TTL wiring lives
 * in {@code RedisConfig} and is not exercised here.
 */
class DictionaryServiceTest {

    private static final String WORD = "ubiquitous";
    private static final String PAYLOAD = "[{\"word\":\"ubiquitous\"}]";
    private static final String UPSTREAM_BASE = "https://api.dictionaryapi.dev/api/v2/entries/en";

    private RestTemplate restTemplate;
    private DictionaryService service;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        CacheManager cacheManager = new ConcurrentMapCacheManager(
                DictionaryService.CACHE_HIT, DictionaryService.CACHE_MISS);
        service = new DictionaryService(restTemplate, cacheManager, UPSTREAM_BASE);
    }

    /**
     * Build the exception RestTemplate ACTUALLY throws for a 404 — the
     * {@code HttpClientErrorException.NotFound} subclass. {@code new HttpClientErrorException(...)}
     * would build the PARENT type, which does not match the {@code catch (... .NotFound)} arm and
     * would silently fall through to the generic catch (caching nothing). This helper is the only
     * way to reproduce production faithfully.
     */
    private static HttpClientErrorException.NotFound notFound() {
        return (HttpClientErrorException.NotFound) HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
    }

    @Test
    void notFound_isRemembered_andSecondLookupDoesNotHitUpstream() {
        when(restTemplate.getForObject(anyString(), eq(String.class), eq(WORD)))
                .thenThrow(notFound());

        assertThat(service.lookup(WORD)).isEqualTo("[]");
        assertThat(service.lookup(WORD)).isEqualTo("[]");

        // The 404 is a stable fact -> exactly one upstream call for two lookups.
        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class), eq(WORD));
    }

    @Test
    void transientFailure_isNotCached_soTheWordCanBeFoundLater() {
        when(restTemplate.getForObject(anyString(), eq(String.class), eq(WORD)))
                .thenThrow(new ResourceAccessException("read timed out"))
                .thenReturn(PAYLOAD);

        // First attempt: upstream times out -> "[]" but NOT remembered.
        assertThat(service.lookup(WORD)).isEqualTo("[]");
        // Second attempt: upstream recovers -> the real payload must come back.
        assertThat(service.lookup(WORD)).isEqualTo(PAYLOAD);
        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class), eq(WORD));
    }

    @Test
    void realPayload_isCached_andServedWithoutASecondCall() {
        when(restTemplate.getForObject(anyString(), eq(String.class), eq(WORD))).thenReturn(PAYLOAD);

        assertThat(service.lookup(WORD)).isEqualTo(PAYLOAD);
        assertThat(service.lookup(WORD)).isEqualTo(PAYLOAD);
        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class), eq(WORD));
    }

    @Test
    void aKnownMiss_isServedEvenAfterUpstreamWouldSucceed() {
        // A miss recorded earlier must win without touching the network at all.
        when(restTemplate.getForObject(anyString(), eq(String.class), eq(WORD)))
                .thenThrow(notFound());
        service.lookup(WORD); // records the miss
        assertThat(service.lookup(WORD)).isEqualTo("[]");
        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class), eq(WORD));
    }

    @Test
    void nullBody_isTreatedAsTransient_notCached() {
        when(restTemplate.getForObject(anyString(), eq(String.class), eq(WORD)))
                .thenReturn(null)
                .thenReturn(PAYLOAD);

        assertThat(service.lookup(WORD)).isEqualTo("[]");
        assertThat(service.lookup(WORD)).isEqualTo(PAYLOAD);
        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class), eq(WORD));
    }

    @Test
    void blankWord_isStillForwarded_lowercaseContractUnchanged() {
        // The controller sanitises + lower-cases before calling; the service must not alter it.
        when(restTemplate.getForObject(anyString(), eq(String.class), eq("hello"))).thenReturn(PAYLOAD);
        assertThat(service.lookup("hello")).isEqualTo(PAYLOAD);
        verify(restTemplate, never()).getForObject(anyString(), eq(String.class), eq(WORD));
    }

    // ── cross-review round: Redis is a cache, not the source of truth ──────────
    @Test
    void cacheWriteFailure_doesNotFabricateNotFound() {
        // The word EXISTS (upstream returned it) but the cache write throws. The caller must still
        // get the real payload — an earlier version put the write inside the try and turned a
        // fetched word into "[]".
        when(restTemplate.getForObject(anyString(), eq(String.class), eq(WORD))).thenReturn(PAYLOAD);
        CacheManager failing = mock(CacheManager.class);
        Cache boom = mock(Cache.class);
        when(failing.getCache(anyString())).thenReturn(boom);
        when(boom.get(anyString())).thenReturn(null);          // cache miss
        doThrow(new RuntimeException("redis write failed")).when(boom).put(anyString(), any());
        DictionaryService svc = new DictionaryService(restTemplate, failing, UPSTREAM_BASE);

        assertThat(svc.lookup(WORD)).isEqualTo(PAYLOAD);       // NOT "[]"
    }

    @Test
    void cacheReadFailure_fallsThroughToUpstream_insteadOf500() {
        // A Redis read error must not escape lookup() (the controller has no handler -> 500).
        when(restTemplate.getForObject(anyString(), eq(String.class), eq(WORD))).thenReturn(PAYLOAD);
        CacheManager failing = mock(CacheManager.class);
        Cache boom = mock(Cache.class);
        when(failing.getCache(anyString())).thenReturn(boom);
        when(boom.get(anyString())).thenThrow(new RuntimeException("redis read failed"));
        DictionaryService svc = new DictionaryService(restTemplate, failing, UPSTREAM_BASE);

        assertThat(svc.lookup(WORD)).isEqualTo(PAYLOAD);       // fell through, no exception
    }
}
