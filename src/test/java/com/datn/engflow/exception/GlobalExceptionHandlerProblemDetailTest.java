package com.datn.engflow.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerProblemDetailTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void accessDeniedUsesRfc7807ProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleAccessDeniedException(
                new AccessDeniedException("Bạn không có quyền xem bài nộp này")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getBody().getTitle()).isEqualTo("Forbidden");
        assertThat(response.getBody().getDetail()).isEqualTo("Bạn không có quyền xem bài nộp này");
    }

    @Test
    void entityNotFoundMapsTo404Not500() {
        ResponseEntity<ProblemDetail> response = handler.handleEntityNotFoundException(
                new EntityNotFoundException("Lesson not found: 1")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
    }

    // -------------------------------------------------------------------
    // audit-v8 Round 1 — F93. POST /api/ai/enrich-word ran the reactive
    // WebClient call to the LOCAL Ollama with a bare .timeout(30s); when the
    // model exceeded that budget (GPU contention during the concurrent AI
    // sweep) the TimeoutException leaked through .block() as
    // reactor.core.Exceptions$ReactiveException and the catch-all mapped it
    // to a generic 500 "Đã xảy ra lỗi hệ thống". A timeout is a transient
    // upstream condition -> 504 Gateway Timeout with a retryable message.
    // -------------------------------------------------------------------

    @Test
    void reactiveTimeoutMapsTo504GatewayTimeout() {
        // reactor.core.Exceptions.propagate wraps a checked TimeoutException in
        // the package-private ReactiveException (a RuntimeException) — exactly
        // what .block() leaks when the WebClient budget expires.
        RuntimeException wrapped = (RuntimeException) reactor.core.Exceptions.propagate(
                new java.util.concurrent.TimeoutException("Did not observe any item within 30000ms"));
        ResponseEntity<ProblemDetail> response = handler.handleRuntimeTimeout(wrapped);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
        assertThat(response.getBody().getTitle()).isEqualTo("Gateway Timeout");
        assertThat(response.getBody().getDetail()).contains("AI");
    }

    @Test
    void plainTimeoutMapsTo504GatewayTimeout() {
        ResponseEntity<ProblemDetail> response = handler.handleTimeoutException(
                new java.util.concurrent.TimeoutException("30s"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
        assertThat(response.getBody().getTitle()).isEqualTo("Gateway Timeout");
    }

    @Test
    void otherReactiveErrorsStayOn500Not504() {
        // audit-v14 F-14-02 SUPERSEDES this expectation for connection errors: a non-timeout
        // reactive error that is NOT an upstream-connect failure still stays on 500. We use a
        // generic IOException that is not a connect-refused, so it must NOT become 503/504.
        RuntimeException wrapped = (RuntimeException) reactor.core.Exceptions.propagate(
                new java.io.IOException("stream closed unexpectedly"));
        ResponseEntity<ProblemDetail> response = handler.handleRuntimeTimeout(wrapped);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getTitle()).isEqualTo("Internal Server Error");
    }

    /**
     * audit-v13 F-13-11 regression: an unknown {@code ?sort=} property on a paged
     * endpoint threw PropertyReferenceException and fell through to a 500.
     * Measured live: GET /api/vocabulary?sort=nonexistentProperty -> HTTP 500.
     * A bad client query parameter must be a 400.
     */
    @Test
    void invalidSortPropertyMapsTo400Not500() {
        // spring-data-commons 4.x moved this class to org.springframework.data.core and
        // its only constructor takes (property, TypeInformation, resolvedPath).
        org.springframework.data.core.PropertyReferenceException ex =
                new org.springframework.data.core.PropertyReferenceException(
                        "nonexistentProperty",
                        org.springframework.data.core.TypeInformation.of(String.class),
                        java.util.List.of());

        ResponseEntity<ProblemDetail> response = handler.handlePropertyReferenceException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }

    /**
     * audit-v14 F-14-02 regression: when the AI upstream (Ollama at :11434) is DOWN, the
     * WebClient connect fails with WebClientRequestException and the app answered a generic
     * 500 "Đã xảy ra lỗi hệ thống". Measured live this session: with Ollama stopped,
     * POST /api/ai/generate-vocab -> HTTP 500. An unreachable UPSTREAM is not a server bug —
     * it is 503 Service Unavailable with a retryable message. This test fails on the old
     * code (which routed it to the 500 catch-all).
     */
    @Test
    void aiUpstreamUnreachableMapsTo503Not500() {
        org.springframework.web.reactive.function.client.WebClientRequestException ex =
                new org.springframework.web.reactive.function.client.WebClientRequestException(
                        new java.io.IOException("Connection refused: host.docker.internal/127.0.0.1:11434"),
                        org.springframework.http.HttpMethod.POST,
                        java.net.URI.create("http://host.docker.internal:11434/v1/chat/completions"),
                        org.springframework.http.HttpHeaders.EMPTY);

        ResponseEntity<ProblemDetail> response = handler.handleWebClientRequestException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getBody().getTitle()).isEqualTo("Service Unavailable");
        assertThat(response.getBody().getDetail()).contains("AI");
    }

    /**
     * F-14-02 guard: a connection-refused wrapped in the reactor RuntimeException (the shape
     * .block() actually leaks) must ALSO map to 503, not the 500 catch-all — the previous
     * test asserted 500 here and that expectation is now superseded.
     */
    @Test
    void reactiveConnectionRefusedMapsTo503() {
        RuntimeException wrapped = (RuntimeException) reactor.core.Exceptions.propagate(
                new java.io.IOException("Connection refused"));
        ResponseEntity<ProblemDetail> response = handler.handleRuntimeTimeout(wrapped);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getTitle()).isEqualTo("Service Unavailable");
    }
}
