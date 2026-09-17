import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")
p = "src/test/java/com/datn/engflow/security/RateLimitFilterTest.java"
s = open(p, encoding="utf-8").read()

anchor = """    @Test
    void whenOverLimit_thenReturns429AndDoesNotChain() throws Exception {"""

extra = """    // audit-v8: the :order bucket was wired to /api/payments/create-order and
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

"""

assert s.count(anchor) == 1, "anchor " + str(s.count(anchor))
s = s.replace(anchor, extra + anchor)
open(p, "w", encoding="utf-8", newline="").write(s)
print("tests added")
