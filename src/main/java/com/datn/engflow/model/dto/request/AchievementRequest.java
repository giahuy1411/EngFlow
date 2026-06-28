package com.datn.engflow.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AchievementRequest {
    @NotBlank(message = "Tên thành tích không được để trống")
    private String name;

    private String description;

    private String badgeType;

    private String iconUrl;

    private Integer pointsRequired;
}
