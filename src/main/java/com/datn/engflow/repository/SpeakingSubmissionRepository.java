package com.datn.engflow.repository;

import com.datn.engflow.model.entity.SpeakingSubmissionStatus;
import com.datn.engflow.model.entity.SpeakingSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Truy cập bảng {@code speaking_submissions} — bản ghi âm/video của phần luyện nói.
 *
 * <p>Hai nhóm truy vấn ở đây phục vụ hai việc khác nhau: học viên xem lại bài của mình
 * (lọc theo {@code userId}) và admin duyệt/chấm (lọc theo {@code status}). Mọi phương
 * thức ở đây đều là đường dùng thật.
 *
 * <p>audit-v21 D-005: hai biến thể trả {@code List} không phân trang
 * ({@code findByUserIdOrderBySubmittedAtDesc}, {@code findAllByOrderBySubmittedAtDesc})
 * đã được XOÁ — 0 call site toàn repo và bản {@code Page} mới là đường dùng thật
 * (bản {@code List} còn rủi ro khi bảng lớn dần).
 */
@Repository
public interface SpeakingSubmissionRepository extends JpaRepository<SpeakingSubmission, Long> {
    /** Bài nói của một user, có phân trang — màn lịch sử luyện nói của học viên. */
    Page<SpeakingSubmission> findByUserId(Long userId, Pageable pageable);
    /** Bài nói của user giới hạn theo một prompt, có phân trang — lọc trong lịch sử. */
    Page<SpeakingSubmission> findByUserIdAndPromptId(Long userId, Long promptId, Pageable pageable);
    /** Bài nói của mọi user theo một prompt, mới nhất trước — admin xem bài của cả lớp. */
    List<SpeakingSubmission> findByPromptIdOrderBySubmittedAtDesc(Long promptId);
    /** Hàng đợi duyệt của admin theo trạng thái (SUBMITTED/GRADED), có phân trang. */
    Page<SpeakingSubmission> findByStatus(SpeakingSubmissionStatus status, Pageable pageable);
    /** Prompt còn bài nộp nào không — dùng để chặn xoá prompt đang có dữ liệu. */
    boolean existsByPromptId(Long promptId);
    /**
     * audit-v7 F55: fallback kiểm tra quyền sở hữu cho URL media kiểu cũ. Media proxy
     * đối chiếu (objectKey, userId) trước khi ký/trả file, tránh lộ bản ghi âm của
     * người khác khi object key không chứa userId.
     */
    boolean existsByMediaObjectKeyAndUserId(String mediaObjectKey, Long userId);
}
