package com.datn.engflow.security;

import java.lang.annotation.*;

/**
 * Annotation đánh dấu endpoint/method chỉ dành cho người dùng Premium.
 *
 * <p><b>Cảnh báo (audit-v20 F-20-11): annotation này hiện KHÔNG được dùng ở đâu cả — 0 usages.</b>
 * Nó chỉ còn là khai báo meta-annotation sót lại; không có {@code HandlerInterceptor}, {@code @Aspect}
 * hay {@code AuthorizationManager} nào đọc nó. Vì vậy gắn nó lên method KHÔNG siết được gì — đừng
 * tin nó thay cho một phép kiểm tra thật.</p>
 *
 * <p>Cơ chế Premium thực tế nằm ở tầng service: gọi {@code UserService.hasPremiumAccess(user)}
 * (nguồn sự thật duy nhất, so {@code premiumExpiry} với clock) — xem {@code SpeakingPromptController}
 * và {@code SpeakingSubmissionController}. Phía frontend dùng route meta {@code requiresPremium}.
 * File này cố tình KHÔNG bị xoá: giữ lại làm dấu vết cho hướng "gate bằng annotation" chưa hoàn
 * thiện, xoá đi sẽ mất ngữ cảnh đó.</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PremiumRequired {
}
