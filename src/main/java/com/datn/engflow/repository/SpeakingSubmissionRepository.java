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
 * (lọc theo {@code userId}) và admin duyệt/chấm (lọc theo {@code status}). Các phương
 * thức trả {@code Page} mới là đường dùng thật; vài biến thể trả {@code List} còn lại
 * không có call site, xem ghi chú ngay trên từng phương thức.
 */
@Repository
public interface SpeakingSubmissionRepository extends JpaRepository<SpeakingSubmission, Long> {
    /** Bài nói của một user, có phân trang — màn lịch sử luyện nói của học viên. */
    Page<SpeakingSubmission> findByUserId(Long userId, Pageable pageable);
    /** Bài nói của user giới hạn theo một prompt, có phân trang — lọc trong lịch sử. */
    Page<SpeakingSubmission> findByUserIdAndPromptId(Long userId, Long promptId, Pageable pageable);
    /**
     * audit-v20 F-20-10: DEAD CODE — 0 call site toàn repo (đã grep). Giữ lại, KHÔNG xoá:
     * có thể là API dự phòng cho màn "bài nói gần đây" khi chưa cần phân trang.
     * Nếu xoá, phải xác nhận lại bằng grep {@code \.<method>(} trước.
     */
    List<SpeakingSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId);
    /** Bài nói của mọi user theo một prompt, mới nhất trước — admin xem bài của cả lớp. */
    List<SpeakingSubmission> findByPromptIdOrderBySubmittedAtDesc(Long promptId);
    /**
     * audit-v20 F-20-10: DEAD CODE — 0 call site toàn repo (đã grep). Giữ lại, KHÔNG xoá:
     * đây là bản không phân trang của danh sách admin, rủi ro nếu bảng lớn dần, nên cân
     * nhắc trước khi dùng lại.
     */
    List<SpeakingSubmission> findAllByOrderBySubmittedAtDesc();
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
