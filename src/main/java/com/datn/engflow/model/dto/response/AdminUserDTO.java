package com.datn.engflow.model.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * class AdminUserDTO.
 */
public class AdminUserDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Boolean isAdmin;
    private Boolean isActive;
    private Integer totalPoints;
    private Integer currentStreak;
    private LocalDateTime createdAt;
    private Boolean isPremium;
    private LocalDate premiumExpiry;
}
