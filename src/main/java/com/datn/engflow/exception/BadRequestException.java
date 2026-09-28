package com.datn.engflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lỗi đầu vào không hợp lệ: dữ liệu thiếu, sai định dạng, vượt giới hạn hoặc
 * thao tác chưa được phép trên dữ liệu hiện có.
 *
 * <p>Nằm ở tầng nghiệp vụ: các service ({@code DeckService}, {@code GameService},
 * {@code MinioService}, {@code ExerciseService}…) ném loại này khi người dùng
 * gửi yêu cầu không hợp lệ, và {@link GlobalExceptionHandler} chuyển nó thành
 * ProblemDetail 400 với đúng thông điệp tiếng Việt gốc.
 *
 * <p>Vì là {@link RuntimeException} nên không cần khai báo {@code throws} ở
 * nơi ném; {@code @ResponseStatus} cho phép mapper dựa trên annotation cũng
 * ánh xạ được sang 400 nếu request không đi qua handler.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    /**
     * Tạo lỗi 400 với thông điệp hiển thị thẳng cho người dùng.
     *
     * @param message thông điệp tiếng Việt sẽ nằm ở trường {@code detail} của ProblemDetail
     */
    public BadRequestException(String message) {
        super(message);
    }
}
