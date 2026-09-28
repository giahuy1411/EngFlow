package com.datn.engflow.model.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Kết quả chấm điểm cho cả lần nộp bài.
 *
 * <p>Tầng response của {@code POST /api/lessons/{lessonId}/exercises/grade} và
 * {@code /submit}. Mẫu số {@code total} chỉ đếm những câu chấm được, nên điểm phần
 * trăm chỉ phản ánh các câu đó; danh sách {@code results} thì giữ đủ mọi câu đã
 * gửi, kể cả câu không chấm được.
 */
@Data
@Builder
public class GradeResponse {
    private List<ExerciseGradeItem> results;
    private int score;
    private int total;
    private double percentage;
}
