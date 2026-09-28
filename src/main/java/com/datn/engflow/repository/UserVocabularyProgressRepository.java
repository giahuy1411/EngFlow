package com.datn.engflow.repository;

import com.datn.engflow.model.entity.UserVocabularyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Truy cập bảng {@code user_vocabulary_progress} — trạng thái SRS của từng từ theo user
 * (mastery level, ease factor, interval, ngày ôn kế tiếp).
 *
 * <p>Bảng có filtered index {@code IX_uvp_due} (audit-v7: srs-due 67ms → 37ms). Hệ quả
 * vận hành: mọi batch DELETE trên bảng này BẮT BUỘC mở đầu bằng
 * {@code SET QUOTED_IDENTIFIER ON;}, thiếu là SQL Server báo {@code Msg 1934} — mà
 * {@code sqlcmd} vẫn trả exit code 0, nên phải quét cả chuỗi {@code Msg} trong output.
 */
@Repository
public interface UserVocabularyProgressRepository extends JpaRepository<UserVocabularyProgress, Long> {
    /**
     * Trạng thái SRS của một từ với một user — tra đơn lẻ khi chấm một lượt ôn
     * ({@code SrsService.reviewWord}).
     */
    Optional<UserVocabularyProgress> findByUserIdAndVocabularyId(Long userId, Long vocabularyId);

    /** Toàn bộ trạng thái SRS của user — nạp một lần cho thống kê/tiến độ deck. */
    List<UserVocabularyProgress> findByUserId(Long userId);

    /**
     * audit-v13 F-13-15: batch lookup để đường due-words lấy mọi dòng tiến độ của một deck
     * trong MỘT truy vấn thay vì một truy vấn mỗi từ (đo được N+1: endpoint due-words phát
     * một query cho mỗi từ trong deck — bằng chứng là delta query-stats, không phải đọc code).
     *
     * <p>Đừng quay lại gọi {@link #findByUserIdAndVocabularyId} trong vòng lặp; có test
     * hồi quy {@code SrsDueWordsAuthzTest} canh đúng điều này.
     */
    List<UserVocabularyProgress> findByUserIdAndVocabularyIdIn(Long userId, java.util.Collection<Long> vocabularyIds);
}
