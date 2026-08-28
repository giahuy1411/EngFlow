package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * class AvatarRequest.
 */
public class AvatarRequest {
    @NotBlank(message = "Avatar URL không được để trống")
    private String avatarUrl;
}
