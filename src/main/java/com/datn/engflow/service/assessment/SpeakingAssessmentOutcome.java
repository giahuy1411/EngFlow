package com.datn.engflow.service.assessment;

import com.datn.engflow.service.assessment.TranscriptAlignmentMetrics.AlignmentResult;

/**
 * Kết quả chấm tự động (phần không phải phát âm) của một bài nộp.
 *
 * <p>Do {@link SpeakingAssessmentService#assess} tạo ra và được
 * {@code SpeakingSubmissionService} tiêu thụ; bên đó chỉ ghi xuống DB những
 * trường khác {@code null}. Trường {@code error} khác {@code null} KHÔNG phải
 * lỗi chí mạng: bài nộp vẫn vào hàng chờ chấm tay kèm lý do hiển thị cho giáo
 * viên, thay vì bị gán một điểm tự động. Nhờ vậy học viên không bao giờ bị 0
 * điểm chỉ vì pipeline tự động hỏng.</p>
 *
 * @param transcript        văn bản nhận dạng được, hoặc văn bản do học viên tự nhập
 * @param transcriptSource  nguồn của transcript: {@code WHISPER}, {@code USER} hoặc {@code NONE}
 * @param alignment         số đo đối chiếu bài mẫu với transcript
 * @param rubric            thang điểm ngôn ngữ do LLM chấm, hoặc {@code null} khi LLM không sẵn sàng
 * @param provider          định danh pipeline đã tạo ra kết quả này
 * @param error             khác {@code null} khi việc chấm bị suy giảm; bài nộp vẫn giữ đường chấm tay
 */
public record SpeakingAssessmentOutcome(
        String transcript,
        String transcriptSource,
        AlignmentResult alignment,
        SpeakingRubricResult rubric,
        String provider,
        String error) {

    /**
     * Tạo kết quả thất bại nhưng vẫn giữ đường chấm tay.
     *
     * <p>Nguồn transcript cố định là {@code NONE} và cả hai trường điểm đều
     * {@code null}, nên bên gọi đọc kết quả này không thể nhầm thất bại với điểm
     * 0. Đây chính là lý do không dùng giá trị 0 thay cho {@code null}.</p>
     *
     * @param provider định danh pipeline
     * @param error    lý do thất bại, dạng người đọc được
     * @return kết quả không mang điểm nào
     */
    public static SpeakingAssessmentOutcome failed(String provider, String error) {
        return new SpeakingAssessmentOutcome(null, "NONE", null, null, provider, error);
    }
}
