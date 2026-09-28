package com.datn.engflow.service.assessment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Số đo đối chiếu thuần văn bản giữa bài mẫu và transcript nhận dạng được.
 *
 * <p>Các chỉ số này được cố ý làm bảo thủ: chúng đo độ phủ nội dung và mức lệch
 * ở cấp từ, không đo chất lượng phát âm. Bên gọi tuyệt đối không được dán nhãn
 * chúng là điểm phát âm.</p>
 *
 * <p>Có hai nơi dùng: {@link SpeakingAssessmentService} cho bài nộp được chấm
 * điểm và {@code ShadowingAiGradingService} cho lượt luyện shadowing. Lớp không
 * thể khởi tạo; {@link #compute} là lối vào duy nhất và {@link AlignmentResult}
 * là đầu ra.</p>
 */
public final class TranscriptAlignmentMetrics {

    private static final int MAX_EDIT_DISTANCE_CELLS = 4_000_000;

    private TranscriptAlignmentMetrics() {
    }

    /**
     * Tính số đo đối chiếu giữa dãy từ bài mẫu và dãy từ nhận dạng được.
     *
     * <p>Đầu vào suy biến được xử lý mà không cần dựng ma trận khoảng cách: bài
     * mẫu rỗng trả về các chỉ số bằng 0 kèm độ dài hypothesis, còn hypothesis
     * rỗng trả về WER 100%, coverage 0% và mọi từ bài mẫu bị tính là deletion.</p>
     *
     * @param referenceText bài mẫu mong đợi (có thể {@code null} với bài nói tự do)
     * @param transcript    lời nói nhận dạng được (có thể {@code null} hoặc chỉ khoảng trắng)
     * @return số đo gồm word error rate, độ phủ bài mẫu và chi tiết các loại sửa
     * @throws IllegalArgumentException khi hai dãy token quá dài so với ma trận khoảng cách
     */
    public static AlignmentResult compute(String referenceText, String transcript) {
        List<String> reference = tokenize(referenceText);
        List<String> hypothesis = tokenize(transcript);

        if (reference.isEmpty()) {
            return new AlignmentResult(0.0, 0.0, 0, 0, 0, 0, hypothesis.size());
        }
        if (hypothesis.isEmpty()) {
            return new AlignmentResult(100.0, 0.0, reference.size(), 0, 0, reference.size(), 0);
        }

        int[][] distance = editDistanceMatrix(reference, hypothesis);
        int[] ops = backtraceOperations(distance, reference, hypothesis);
        int substitutions = ops[0];
        int insertions = ops[1];
        int deletions = ops[2];
        int correct = reference.size() - deletions - substitutions;

        double wer = 100.0 * (substitutions + insertions + deletions) / reference.size();
        double coverage = 100.0 * correct / reference.size();
        return new AlignmentResult(
                round(wer), round(coverage), correct, substitutions, insertions, deletions, hypothesis.size());
    }

    private static List<String> tokenize(String text) {
        List<String> words = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return words;
        }
        // Chuẩn hoá WER thông thường: hạ chữ thường và bỏ dấu câu (kể cả dấu nháy
        // đơn), để "Don't" và "dont" khớp thành cùng một token.
        String normalized = text.toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("['\u2018\u2019`]", "");
        for (String raw : normalized.split("[^\\p{L}\\p{N}]+")) {
            if (!raw.isEmpty()) {
                words.add(raw);
            }
        }
        return words;
    }

    /**
     * Dựng ma trận Levenshtein đầy đủ trên hai dãy token.
     *
     * <p>Số ô được kiểm tra với {@link #MAX_EDIT_DISTANCE_CELLS} trước khi cấp
     * phát: một transcript dài bất thường sẽ đòi hàng gigabyte và hạ cả JVM nếu
     * không chặn. Phép nhân được tính trong {@code long} để bản thân nó không
     * tràn số.</p>
     *
     * @param reference  dãy token mong đợi
     * @param hypothesis dãy token nhận dạng được
     * @return ma trận kích thước {@code (reference+1) x (hypothesis+1)}
     * @throws IllegalArgumentException khi số ô vượt ngưỡng
     */
    private static int[][] editDistanceMatrix(List<String> reference, List<String> hypothesis) {
        if ((long) (reference.size() + 1) * (hypothesis.size() + 1) > MAX_EDIT_DISTANCE_CELLS) {
            throw new IllegalArgumentException("Transcript too long for alignment metrics");
        }
        int[][] distance = new int[reference.size() + 1][hypothesis.size() + 1];
        for (int i = 0; i <= reference.size(); i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= hypothesis.size(); j++) {
            distance[0][j] = j;
        }
        for (int i = 1; i <= reference.size(); i++) {
            for (int j = 1; j <= hypothesis.size(); j++) {
                int cost = reference.get(i - 1).equals(hypothesis.get(j - 1)) ? 0 : 1;
                distance[i][j] = Math.min(
                        Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                        distance[i - 1][j - 1] + cost);
            }
        }
        return distance;
    }

    /**
     * Truy vết ngược từ góc dưới-phải của ma trận để tách khoảng cách sửa thành
     * substitution, insertion và deletion.
     *
     * <p>Ưu tiên đi chéo, rồi deletion, rồi insertion, nên khi hoà thì luôn tính
     * là substitution thay vì một cặp delete+insert — nhờ đó ba con số cộng lại
     * đúng bằng WER đã báo cáo. Chỉ số hàng hoặc cột còn dư ở cuối được cộng vào
     * deletion hoặc insertion tương ứng; việc này chỉ xảy ra khi dãy này là tiền
     * tố của dãy kia.</p>
     *
     * @param distance   ma trận do {@link #editDistanceMatrix} tạo ra
     * @param reference  dãy token mong đợi
     * @param hypothesis dãy token nhận dạng được
     * @return mảng ba phần tử {@code {substitutions, insertions, deletions}}
     */
    private static int[] backtraceOperations(int[][] distance, List<String> reference, List<String> hypothesis) {
        int substitutions = 0;
        int insertions = 0;
        int deletions = 0;
        int i = reference.size();
        int j = hypothesis.size();
        while (i > 0 && j > 0) {
            int current = distance[i][j];
            if (current == distance[i - 1][j - 1] + (reference.get(i - 1).equals(hypothesis.get(j - 1)) ? 0 : 1)) {
                if (!reference.get(i - 1).equals(hypothesis.get(j - 1))) {
                    substitutions++;
                }
                i--;
                j--;
            } else if (current == distance[i - 1][j] + 1) {
                deletions++;
                i--;
            } else {
                insertions++;
                j--;
            }
        }
        deletions += i;
        insertions += j;
        return new int[]{substitutions, insertions, deletions};
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /**
     * Kết quả đối chiếu của một bài nộp.
     *
     * @param wordErrorRatePercent mức lệch so với bài mẫu (0 = khớp hoàn toàn)
     * @param coveragePercent      tỉ lệ từ trong bài mẫu được nói đúng
     * @param correctWords         số từ bài mẫu khớp
     * @param substitutions        số từ bài mẫu bị thay bằng từ khác
     * @param insertions           số từ thừa không có trong bài mẫu
     * @param deletions            số từ bài mẫu bị thiếu trong transcript
     * @param hypothesisWords      tổng số từ nhận dạng được
     */
    public record AlignmentResult(
            double wordErrorRatePercent,
            double coveragePercent,
            int correctWords,
            int substitutions,
            int insertions,
            int deletions,
            int hypothesisWords) {
    }
}
