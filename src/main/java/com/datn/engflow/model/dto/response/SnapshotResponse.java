package com.datn.engflow.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Kết quả chạy một job có mốc thời gian.
 *
 * <p>Hiện chưa có controller nào trả kiểu này — luồng đọc dùng
 * {@link StudySnapshot} cho {@code GET /api/streak/snapshot}. DTO được giữ lại làm
 * shape chung cho các job chạy nền có thể trả về sau này.
 */
@Data
@Builder
@AllArgsConstructor
public class SnapshotResponse {
    private Long id;
    private LocalDateTime createdAt;
}
