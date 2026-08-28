package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.entity.User;

/**
 * record UserSummaryResponse.
 */
public record UserSummaryResponse(
        Long id,
        String fullName,
        String avatarUrl
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getFullName(), user.getAvatarUrl());
    }
}
