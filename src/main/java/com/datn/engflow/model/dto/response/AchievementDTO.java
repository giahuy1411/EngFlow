package com.datn.engflow.model.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementDTO {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private String badgeType;
}
