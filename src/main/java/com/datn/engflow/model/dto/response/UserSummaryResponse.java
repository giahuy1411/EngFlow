package com.datn.engflow.model.dto.response;

import com.datn.engflow.model.entity.User;

/**
 * Định danh người dùng ở dạng rút gọn, dùng làm tham chiếu lồng nhau.
 *
 * <p>Tầng response: {@link SpeakingSubmissionResponse} nhúng bản này cho người nộp
 * và người chấm, nên không cần tải profile đầy đủ. Chỉ ba trường — cùng bộ với
 * {@code UserResponse} nhưng bỏ hết thông tin tài khoản và quyền.
 */
public record UserSummaryResponse(
        Long id,
        String fullName,
        String avatarUrl
) {
    /**
     * Rút gọn entity User xuống ba trường hiển thị.
     *
     * @param user entity user, phải khác null
     * @return bản tóm tắt của user
     */
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getFullName(), user.getAvatarUrl());
    }
}
