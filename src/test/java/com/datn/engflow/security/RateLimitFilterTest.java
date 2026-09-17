package com.datn.engflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(redisTemplate);
    }

    @Test
    void whenRedisThrows_thenFailOpenAndChainContinues() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("Redis down"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void whenRedisThrowsOnExpire_thenFailOpen() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/lessons");
        when(request.getRemoteAddr()).thenReturn("5.6.7.8");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        doThrow(new RuntimeException("expire fail")).when(redisTemplate).expire(anyString(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void whenUnderLimit_thenChainContinues() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(2L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    // audit-v8: the :order bucket was wired to /api/payments/create-order and
    // /api/premium, paths that no controller serves, so 13 order requests in a
    // row all landed in the 100/min global bucket instead of the intended 10/min.
    @Test
    void orderEndpointUsesTheOrderBucketAndItsTenPerMinuteCeiling() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/payment/create-order");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("9.9.9.9");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(11L);
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, never()).doFilter(any(), any());
        org.mockito.ArgumentCaptor<String> key = org.mockito.ArgumentCaptor.captor();
        verify(valueOps).increment(key.capture());
        assertThatOrder(key.getValue());
    }

    private static void assertThatOrder(String redisKey) {
        if (!redisKey.endsWith(":order")) {
            throw new AssertionError("expected the :order bucket but got " + redisKey);
        }
    }

    @Test
    void aiEndpointUsesTheAiBucket() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/ai/enrich-word");
        // generation is a POST and only POST consumes the :ai budget
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("8.8.8.8");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(21L);
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        org.mockito.ArgumentCaptor<String> key = org.mockito.ArgumentCaptor.captor();
        verify(valueOps).increment(key.capture());
        if (!key.getValue().endsWith(":ai")) {
            throw new AssertionError("expected the :ai bucket but got " + key.getValue());
        }
    }

    @Test
    void orderEndpointUnderCeilingStillPasses() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/payment/create-order");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("9.9.9.9");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(5L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void whenOverLimit_thenReturns429AndDoesNotChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(21L);
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ---------------------------------------------------------------------
    // audit-v8 Round 1 — F91/F92. Bucket routing was derived from literal
    // prefixes that only matched ONE controller each, so every OTHER endpoint
    // doing the same expensive work fell through to the 100/min :global
    // bucket. Measured live in sweep/v8/p8_bucket_coverage.js (redis key
    // inspected after each prefix); these tests pin the routing so a future
    // path rename cannot silently reopen the hole.
    // ---------------------------------------------------------------------

    private void assertBucketFor(String method, String uri, String expectedSuffix) throws Exception {
        RateLimitFilter fresh = new RateLimitFilter(redisTemplate);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        // a FRESH ValueOperations per call: the shared @Mock would accumulate
        // invocations across the assertions inside one test method.
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(req.getRequestURI()).thenReturn(uri);
        when(req.getRemoteAddr()).thenReturn("7.7.7.7");
        if (!"GET".equals(method)) {
            when(req.getMethod()).thenReturn(method);
        }
        when(redisTemplate.opsForValue()).thenReturn(ops);
        // 1L keeps the request under every threshold while still writing the key
        when(ops.increment(anyString())).thenReturn(1L);

        fresh.doFilterInternal(req, res, chain);

        org.mockito.ArgumentCaptor<String> key = org.mockito.ArgumentCaptor.captor();
        verify(ops).increment(key.capture());
        String got = key.getValue();
        if (!got.endsWith(expectedSuffix)) {
            throw new AssertionError("uri=" + uri + " expected bucket " + expectedSuffix + " but got " + got);
        }
    }

    @Test
    void aiExerciseGenerationEndpointsUseTheAiBucketNotGlobal() throws Exception {
        // 6 endpoints that call Ollama but live under /api/admin/exercises/ai/
        assertBucketFor("POST", "/api/admin/exercises/ai/generate-async", ":ai");
        assertBucketFor("POST", "/api/admin/exercises/ai/generate", ":ai");
        assertBucketFor("POST", "/api/admin/exercises/ai/generate-all", ":ai");
        assertBucketFor("POST", "/api/admin/exercises/ai/generate-batch", ":ai");
        assertBucketFor("POST", "/api/admin/exercises/ai/validate", ":ai");
        assertBucketFor("POST", "/api/admin/exercises/ai/backfill-answers", ":ai");
    }

    @Test
    void aiSpeakingAndVideoPromptEndpointsUseTheAiBucket() throws Exception {
        assertBucketFor("POST", "/api/v1/admin/speaking-prompts/ai-generate", ":ai");
        assertBucketFor("POST", "/api/v1/admin/speaking-prompts/ai-generate-full", ":ai");
        // the video-* spelling is an ALIAS of the same handler and must route identically
        assertBucketFor("POST", "/api/v1/admin/video-prompts/ai-generate", ":ai");
        assertBucketFor("POST", "/api/v1/admin/video-prompts/ai-generate-full", ":ai");
    }

    @Test
    void aiVideoLessonAndAiGradeEndpointsUseTheAiBucket() throws Exception {
        assertBucketFor("POST", "/api/v1/admin/video-lessons/translate-transcript", ":ai");
        assertBucketFor("POST", "/api/v1/admin/video-lessons/fetch-youtube", ":ai");
        assertBucketFor("POST", "/api/v1/admin/video-attempts/42/ai-grade", ":ai");
    }

    @Test
    void theLiteralApiAiPrefixStillUsesTheAiBucket() throws Exception {
        // control: the original prefix must keep working
        assertBucketFor("POST", "/api/ai/generate-vocab", ":ai");
        assertBucketFor("POST", "/api/ai/enrich-word", ":ai");
        assertBucketFor("POST", "/api/ai/save-vocab", ":ai");
    }

    @Test
    void avatarAndVideoLessonUploadsUseTheUploadBucket() throws Exception {
        assertBucketFor("POST", "/api/auth/avatar/upload", ":upload");
        assertBucketFor("POST", "/api/v1/admin/video-lessons/upload", ":upload");
        // controls that already worked
        assertBucketFor("POST", "/api/admin/audio-upload", ":upload");
        assertBucketFor("POST", "/api/admin/upload", ":upload");
    }

    @Test
    void unrelatedEndpointsKeepFallingBackToGlobal() throws Exception {
        // guard against over-broad matching: these must NOT be pulled into :ai/:upload
        assertBucketFor("GET", "/api/lessons", ":global");
        assertBucketFor("GET", "/api/admin/exercises", ":global");
        assertBucketFor("POST", "/api/lessons/445/exercises/submit", ":global");
        // GET on the AI prefixes is a read, not generation -> stays global
        assertBucketFor("GET", "/api/admin/exercises/ai/status", ":global");
        // DELETE/DELETE-family on an AI path must not be swept in either
        assertBucketFor("DELETE", "/api/admin/exercises/ai/generate-async", ":global");
    }
    @Test
    void whenExpireFails_thenKeyDeletedBestEffortAndChainContinues() throws Exception {
        // EXPIRE lỗi sau INCR thành công: key không được rò rỉ mất TTL (kẹt 429 vĩnh viễn
        // cho IP+bucket đó). Filter tự dọn key và request hiện tại vẫn fail-open.
        when(request.getRequestURI()).thenReturn("/api/lessons");
        when(request.getRemoteAddr()).thenReturn("5.6.7.8");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        doThrow(new RuntimeException("expire fail")).when(redisTemplate).expire(anyString(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(redisTemplate).delete("rate_limit:5.6.7.8:global");
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }}
