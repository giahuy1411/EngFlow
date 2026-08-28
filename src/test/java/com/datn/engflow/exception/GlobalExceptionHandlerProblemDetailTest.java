package com.datn.engflow.exception;

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
}
