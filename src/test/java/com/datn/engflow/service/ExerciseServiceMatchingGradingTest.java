package com.datn.engflow.service;

import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.dto.response.GradeResponse;
import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.model.enums.LessonLevel;
import com.datn.engflow.repository.ExerciseAttemptRepository;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * audit-v10 F127 — chấm bài MATCHING.
 *
 * <p>Trước bản sửa này, {@code ExerciseService} chấm MATCHING bằng so khớp CHUỖI
 * giữa câu trả lời và {@code correct_answer}. Đo trên dữ liệu sống: 330/331 row
 * MATCHING đã published lưu {@code correct_answer} dạng CHỮ, trong khi
 * {@code MatchingExercise.vue} gửi lên dạng CHỈ SỐ VỊ TRÍ. Hai chuỗi không bao
 * giờ bằng nhau — thử 4 bài published, gửi đúng định dạng client gửi, cả 4 đều
 * {@code correct=false}. Tức học sinh nối đúng hết vẫn 0 điểm.
 *
 * <p>Và sửa định dạng chỉ số là BẤT KHẢ THI: client xáo trộn cột phải, nên chỉ
 * số hiển thị không bằng chỉ số gốc. Server không dựng lại được phép hoán vị.
 *
 * <p>Nên hợp đồng mới là: client gửi cặp CHỮ, server đối chiếu với {@code options}
 * (mỗi phần tử {@code "left|right"}), so khớp theo TẬP HỢP nên thứ tự nối không
 * ảnh hưởng.
 */
@SpringBootTest
@Transactional
class ExerciseServiceMatchingGradingTest {

    @Autowired private ExerciseService exerciseService;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private ExerciseAttemptRepository exerciseAttemptRepository;
    @Autowired private UserRepository userRepository;

    private Lesson lesson;

    @BeforeEach
    void setup() {
        exerciseAttemptRepository.deleteAll();
        lesson = lessonRepository.save(Lesson.builder()
                .title("ZZ matching grading")
                .content("<p>x</p>")
                .level(LessonLevel.ELEMENTARY)
                .isPublished(true)
                .build());
    }

    private Exercise seedMatching(String options, String correctAnswer) {
        return exerciseRepository.save(Exercise.builder()
                .lesson(lesson)
                .question("Match the pairs")
                .exerciseType(ExerciseType.MATCHING)
                .options(options)
                .correctAnswer(correctAnswer)
                .orderIndex(0)
                .build());
    }

    private GradeResponse grade(Exercise ex, String userAnswer) {
        GradeRequest req = new GradeRequest();
        GradeRequest.AnswerItem item = new GradeRequest.AnswerItem();
        item.setExerciseId(ex.getId());
        item.setUserAnswer(userAnswer);
        req.setAnswers(List.of(item));
        return exerciseService.gradeExercises(lesson.getId(), req);
    }

    // ---------- Hợp đồng mới: gửi CHỮ ----------

    @Test
    void letterPairs_correctOrder_scoresFull() {
        Exercise ex = seedMatching("[\"A|B\",\"B|D\",\"C|C\",\"D|A\"]", "A=B,B=D,C=C,D=A");
        GradeResponse r = grade(ex, "A=B,B=D,C=C,D=A");
        assertThat(r.getResults().get(0).isCorrect()).isTrue();
        assertThat(r.getScore()).isEqualTo(1);
        assertThat(r.getTotal()).isEqualTo(1);
    }

    @Test
    void letterPairs_shuffledOrder_stillScoresFull() {
        // "Nối cặp" không phụ thuộc thứ tự người học bấm. Đây chính là điều làm
        // cho việc chấm theo chỉ số trở nên vô nghĩa.
        Exercise ex = seedMatching("[\"A|B\",\"B|D\",\"C|C\",\"D|A\"]", "A=B,B=D,C=C,D=A");
        GradeResponse r = grade(ex, "D=A,C=C,A=B,B=D");
        assertThat(r.getResults().get(0).isCorrect()).isTrue();
    }

    @Test
    void wordPairs_scoreCorrectly() {
        // Đây là định dạng của 330/331 row thật.
        Exercise ex = seedMatching(
                "[\"be | is/are\", \"have got | possess\"]",
                "word1=is/are word2=possess");
        GradeResponse r = grade(ex, "be=is/are,have got=possess");
        assertThat(r.getResults().get(0).isCorrect()).isTrue();
    }

    @Test
    void caseAndSpaceInsensitive() {
        Exercise ex = seedMatching("[\"A|B\",\"B|D\"]", "A=B,B=D");
        GradeResponse r = grade(ex, "  a = b , B = d  ");
        assertThat(r.getResults().get(0).isCorrect()).isTrue();
    }

    // ---------- Phải SAI ----------

    @Test
    void wrongPairing_isIncorrect() {
        Exercise ex = seedMatching("[\"A|B\",\"B|D\",\"C|C\",\"D|A\"]", "A=B,B=D,C=C,D=A");
        GradeResponse r = grade(ex, "A=B,B=D,C=D,D=A"); // C và D đổi chỗ
        assertThat(r.getResults().get(0).isCorrect()).isFalse();
    }

    @Test
    void partialPairs_isIncorrect() {
        // Nối đúng 2/4 chưa phải hoàn thành bài; không được chấp nhận tập con.
        Exercise ex = seedMatching("[\"A|B\",\"B|D\",\"C|C\",\"D|A\"]", "A=B,B=D,C=C,D=A");
        GradeResponse r = grade(ex, "A=B,B=D");
        assertThat(r.getResults().get(0).isCorrect()).isFalse();
    }

    @Test
    void indexForm_isIncorrectNow() {
        // Hợp đồng cũ (chỉ số) KHÔNG còn được chấp nhận: nó không mang thông tin
        // gì về cặp thật, nên không thể coi là đúng.
        Exercise ex = seedMatching("[\"A|B\",\"B|D\",\"C|C\",\"D|A\"]", "A=B,B=D,C=C,D=A");
        GradeResponse r = grade(ex, "0=0,1=1,2=2,3=3");
        assertThat(r.getResults().get(0).isCorrect()).isFalse();
    }

    // ---------- Ungradeable ----------

    @Test
    void matchingWithUnusableOptions_isUngradeableNotWrong() {
        // options không có cặp "left|right" -> không chấm được. Phải được LOẠI
        // khỏi tử số và mẫu số, không được tính là sai — cùng chính sách với
        // correct_answer rỗng.
        Exercise ex = seedMatching("[\"khong co dau pipe\",\"cung khong\"]", "A=B,B=D");
        GradeResponse r = grade(ex, "A=B,B=D");
        assertThat(r.getResults().get(0).isUngradeable()).isTrue();
        assertThat(r.getTotal()).isZero();
    }

    @Test
    void matchingWithNullOptions_isUngradeable() {
        Exercise ex = seedMatching(null, "A=B,B=D");
        GradeResponse r = grade(ex, "A=B,B=D");
        assertThat(r.getResults().get(0).isUngradeable()).isTrue();
        assertThat(r.getTotal()).isZero();
    }

    @Test
    void matchingWithMalformedOptionsJson_isUngradeable() {
        Exercise ex = seedMatching("{khong-phai-mang}", "A=B,B=D");
        GradeResponse r = grade(ex, "A=B,B=D");
        assertThat(r.getResults().get(0).isUngradeable()).isTrue();
        assertThat(r.getTotal()).isZero();
    }

    // ---------- Các loại khác KHÔNG bị ảnh hưởng ----------

    @Test
    void nonMatchingStillUsesStringComparison() {
        Exercise ex = exerciseRepository.save(Exercise.builder()
                .lesson(lesson)
                .question("The dog ___ in the yard.")
                .exerciseType(ExerciseType.FILL_BLANK)
                .options("[]")
                .correctAnswer("is")
                .orderIndex(1)
                .build());
        GradeResponse r = grade(ex, "  IS ");
        assertThat(r.getResults().get(0).isCorrect()).isTrue();
        assertThat(r.getResults().get(0).isUngradeable()).isFalse();
    }

    @Test
    void nonMatchingWithEmptyAnswerStillUngradeable() {
        Exercise ex = exerciseRepository.save(Exercise.builder()
                .lesson(lesson)
                .question("q")
                .exerciseType(ExerciseType.FILL_BLANK)
                .correctAnswer("")
                .orderIndex(2)
                .build());
        GradeResponse r = grade(ex, "");
        assertThat(r.getResults().get(0).isUngradeable()).isTrue();
        assertThat(r.getTotal()).isZero();
    }
}
