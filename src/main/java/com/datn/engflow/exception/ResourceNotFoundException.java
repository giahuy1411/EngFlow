package com.datn.engflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Không tìm thấy bản ghi mà một thao tác cần tới: thường là thêm
 * {@code orElseThrow(() -> new ResourceNotFoundException(...))} ngay sau lời gọi
 * repository trong service.
 *
 * <p>Thông điệp được ghép sẵn từ ba tham số theo khuôn
 * {@code "%s not found with %s : '%s'"} nên không cần tự chuẩn hoá khi ném.
 * {@link GlobalExceptionHandler} ánh xạ sang ProblemDetail 404.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Tạo lỗi 404 từ tên tài nguyên, tên trường định danh và giá trị định danh.
     *
     * @param resourceName tên tài nguyên, ví dụ {@code "Lesson"}, {@code "User"}
     * @param fieldName   tên trường khoá, ví dụ {@code "id"}, {@code "email"}
     * @param fieldValue  giá trị khoá không tìm thấy
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}
