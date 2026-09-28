package com.datn.engflow.exception;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO mô tả thân phản lỗi dạng JSON phẳng: mã HTTP, một thông điệp và bản đồ
 * lỗi theo từng trường.
 *
 * <p>Chưa có nơi nào trong cây nguồn tạo hay đọc kiểu này — hợp đồng lỗi đang
 * chạy thật là RFC 7807 {@code ProblemDetail} do {@link GlobalExceptionHandler}
 * dựng. Giữ khai báo này cho các payload lỗi không theo ProblemDetail.
 */
@Data
@Builder
public class ErrorResponse {
    /** mã trạng thái HTTP, ví dụ 400 hoặc 404 */
    private int status;
    /** thông điệp tổng quát hiển thị cho người dùng */
    private String message;
    /** lỗi chi tiết theo tên trường, rỗng khi lỗi không gắn với trường cụ thể */
    private Map<String, String> errors;
}
