package com.datn.engflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Central exception handler returning RFC 7807 ProblemDetail responses.
 * Consistent error contract across the API (AGENTS.md rule).
 */
@Slf4j
@RestControllerAdvice
/**
 * class GlobalExceptionHandler.
 */
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ProblemDetail> handleBadRequestException(BadRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * State conflicts (duplicate email/username on register) must return 409,
     * not fall through to the 500 catch-all.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflictException(ConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * audit-v7 F62: `Level.valueOf("FOO")` (query/body enum params) threw
     * IllegalArgumentException and fell through to the 500 catch-all. Bad input
     * is a client error → 400 with the Vietnamese message the frontend shows.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Bad argument: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Giá trị không hợp lệ. Vui lòng kiểm tra lại đầu vào.");
        problem.setTitle("Bad Request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * JPA-level "not found" (thrown by ExerciseService and any repository orElseThrow)
     * must surface as 404, not fall through to the catch-all 500.
     *
     * @param ex entity not found raised by the persistence layer
     * @return not-found problem details
     */
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu.");
        problem.setTitle("Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = (error instanceof FieldError fieldError) ? fieldError.getField() : error.getObjectName();
            validationErrors.put(fieldName, error.getDefaultMessage());
        });

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ");
        problem.setTitle("Validation Failed");
        problem.setProperty("errors", validationErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Bean Validation trên tham số phương thức — gồm {@code List<@Valid T>} vì
     * {@code @Valid} đặt trên chính tham số không cascade vào phần tử List.
     * Spring 6.1+ ném loại này (không phải MethodArgumentNotValidException);
     * nếu không có handler riêng nó rơi vào catch-all và trả 500 thay vì 400.
     */
    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleHandlerMethodValidationException(
            org.springframework.web.method.annotation.HandlerMethodValidationException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getParameterValidationResults().forEach(result ->
                result.getResolvableErrors().forEach(error -> {
                    String code = error.getCodes() != null && error.getCodes().length > 0
                            ? error.getCodes()[error.getCodes().length - 1] : "invalid";
                    validationErrors.put(code, error.getDefaultMessage());
                }));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ");
        problem.setTitle("Validation Failed");
        problem.setProperty("errors", validationErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác.");
        problem.setTitle("Unauthorized");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("JSON Parse Error: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu JSON không hợp lệ hoặc sai định dạng.");
        problem.setTitle("Malformed Request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotSupportedException(org.springframework.web.HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Loại nội dung không được hỗ trợ. Dùng application/json.");
        problem.setTitle("Unsupported Media Type");
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problem);
    }

    /**
     * JSON body posted to a multipart endpoint (e.g. video shadowing attempts)
     * must be a 4xx client error, not fall through to the catch-all 500.
     */
    @ExceptionHandler({org.springframework.web.multipart.MultipartException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<ProblemDetail> handleMultipartException(Exception ex) {
        log.warn("Malformed request (missing part/parameter): {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Yêu cầu thiếu tham số hoặc cần dạng multipart/form-data đầy đủ (bao gồm tệp đính kèm).");
        problem.setTitle("Bad Request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Converts authorization failures to an RFC 7807 response.
     *
     * @param ex access denial raised by the service layer
     * @return forbidden problem details
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Forbidden");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * audit-v5 fix: controllers (SpeakingSubmission, VideoLesson premium gates)
     * throw ResponseStatusException(403/404/...) but no handler existed, so the
     * catch-all {@link #handleGlobalException} converted them to 500. Propagate
     * the intended status instead.
     *
     * @param ex status-carrying exception raised by a controller or service
     * @return problem details with the original HTTP status
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(
            org.springframework.web.server.ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase());
        problem.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(NoResourceFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu.");
        problem.setTitle("Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED,
                "Phương thức HTTP không được hỗ trợ cho endpoint này.");
        problem.setTitle("Method Not Allowed");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problem);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatchException(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        String param = ex.getName();
        String value = String.valueOf(ex.getValue());
        String message = "Tham số '" + param + "' với giá trị '" + value + "' không hợp lệ.";
        log.warn("Type mismatch: {}", message);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        problem.setTitle("Bad Request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * audit-v8 F93: AI endpoints (e.g. {@code POST /api/ai/enrich-word}) run the
     * reactive WebClient call to the local Ollama with a bare 30s timeout. Under
     * GPU contention (a concurrent AI sweep) the model can exceed that budget and
     * {@code .block()} leaks a {@code reactor.core.Exceptions$ReactiveException}
     * (package-private, extends RuntimeException) wrapping
     * {@code java.util.concurrent.TimeoutException} — the catch-all used to turn
     * that into a generic 500. A timeout is a transient upstream condition, not a
     * server fault: map ONLY the TimeoutException cause chain to 504 Gateway
     * Timeout with a retryable message; every other error keeps the 500 path.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleRuntimeTimeout(RuntimeException ex) {
        if (containsTimeout(ex)) {
            log.warn("AI upstream timeout: {}", ex.getMessage());
            return timeoutProblem();
        }
        return problem500(ex);
    }

    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public ResponseEntity<ProblemDetail> handleTimeoutException(java.util.concurrent.TimeoutException ex) {
        log.warn("Timeout: {}", ex.getMessage());
        return timeoutProblem();
    }

    private static boolean containsTimeout(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private ResponseEntity<ProblemDetail> timeoutProblem() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT,
                "AI đang chậm phản hồi hoặc quá tải. Vui lòng thử lại sau.");
        problem.setTitle("Gateway Timeout");
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(Exception ex) {
        return problem500(ex);
    }

    private ResponseEntity<ProblemDetail> problem500(Exception ex) {
        log.error("Internal server error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
        problem.setTitle("Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
