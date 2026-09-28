package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO đổi ảnh đại diện, bind từ body của {@code PUT /api/auth/avatar}.
 *
 * <p>Trường này chỉ nhận URL đã có, không nhận file: nhánh upload ảnh là
 * {@code POST /api/auth/avatar/upload}, đẩy lên Cloudinary rồi mới gọi lại endpoint này
 * với URL trả về.
 */
@Data
public class AvatarRequest {
    @NotBlank(message = "Avatar URL không được để trống")
    private String avatarUrl;
}
