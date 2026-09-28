package com.datn.engflow.exception;

/**
 * Raised when a request conflicts with existing state — e.g. registering a
 * username or email that already exists. Mapped to HTTP 409 by
 * {@link GlobalExceptionHandler} (RFC 7807).
 */
public class ConflictException extends RuntimeException {

    /**
     * Tạo lỗi 409 với thông điệp mô tả xung đột trạng thái.
     *
     * @param message thông điệp tiếng Việt sẽ nằm ở trường {@code detail} của ProblemDetail
     */
    public ConflictException(String message) {
        super(message);
    }
}
