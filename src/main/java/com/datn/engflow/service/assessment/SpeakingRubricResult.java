package com.datn.engflow.service.assessment;

/**
 * Thang điểm chất lượng ngôn ngữ do LLM cục bộ chấm từ transcript.
 *
 * <p>Điểm dùng đúng thang 0-10 đang lưu ở {@code score_grammar},
 * {@code score_vocabulary} và {@code score_fluency}. Model không hề nghe audio,
 * nên các điểm này mô tả nội dung chứ không phải phát âm — đừng gọi chúng là
 * điểm phát âm.</p>
 *
 * @param grammar    độ chính xác ngữ pháp, 0-10
 * @param vocabulary độ phong phú và mức phù hợp của từ ngữ, 0-10
 * @param fluency    độ mạch lạc và tự nhiên suy ra từ transcript, 0-10
 * @param feedback   nhận xét ngắn cho học viên bằng tiếng Việt
 */
public record SpeakingRubricResult(
        int grammar,
        int vocabulary,
        int fluency,
        String feedback) {

    /**
     * Lấy trung bình ba chiều của rubric về đúng thang 0-10 mà giáo viên đang
     * dùng, làm tròn một chữ số thập phân.
     *
     * @return điểm rubric tổng, trong khoảng 0.0 đến 10.0
     */
    public double total() {
        return Math.round((grammar + vocabulary + fluency) / 3.0 * 10.0) / 10.0;
    }
}
