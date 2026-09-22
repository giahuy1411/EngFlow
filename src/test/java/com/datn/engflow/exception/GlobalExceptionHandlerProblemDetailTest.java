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
        // connection refused is NOT a timeout -> must keep the existing 500 path
        RuntimeException wrapped = (RuntimeException) reactor.core.Exceptions.propagate(
                new java.io.IOException("Connection refused"));
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
}
