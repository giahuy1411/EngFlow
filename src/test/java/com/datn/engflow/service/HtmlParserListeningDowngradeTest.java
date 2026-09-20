package com.datn.engflow.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * audit-v10 — hạ nhãn LISTENING khi row thực chất là trắc nghiệm.
 *
 * <p><b>Vì sao cần:</b> {@code detectDiviExerciseType} gán nhãn theo SECTION và
 * quy tắc đầu tiên là "section có thẻ {@code <audio>} → LISTENING". Nhưng thẻ
 * đó có thể có trong HTML nguồn mà FILE không tải về được — khi đó row mang
 * nhãn LISTENING vĩnh viễn dù {@code audio_url} rỗng.
 *
 * <p><b>Đo được 2026-09-20:</b> 9 row như vậy trong DB. Trong đó 7 row có
 * {@code options} là mảng JSON và {@code correct_answer} trùng khít một lựa
 * chọn — tức là trắc nghiệm. UI hiện nhãn "NGHE" kèm nút "🔊 Nghe" đọc to chính
 * câu lệnh ("I can understand a text about brothers and sisters").
 *
 * <p>Test này kiểm phần quyết định: khi nào một chuỗi {@code options} được coi
 * là "có lựa chọn thật". Đây là điều kiện duy nhất của việc hạ nhãn, nên nó
 * phải đúng ở cả hai phía — nhận mảng thật, và TỪ CHỐI mọi thứ khác.
 */
class HtmlParserListeningDowngradeTest {

    /**
     * Gọi thẳng hàm private — nó là toàn bộ logic quyết định.
     *
     * {@code HtmlParserService} không có dependency nào (constructor mặc định),
     * nên khởi tạo trực tiếp được, không cần Spring context.
     */
    private boolean hasRealChoices(String options) throws Exception {
        HtmlParserService service = new HtmlParserService();
        Method m = HtmlParserService.class.getDeclaredMethod("hasRealChoices", String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(service, options);
    }

    // ---------- Phải NHẬN ----------

    @Test
    void jsonArrayWithTwoChoicesIsReal() throws Exception {
        assertThat(hasRealChoices("[\"a\", \"b\"]")).isTrue();
    }

    @Test
    void jsonArrayWithFourChoicesIsReal() throws Exception {
        assertThat(hasRealChoices("[\"A. one\", \"B. two\", \"C. three\", \"D. four\"]")).isTrue();
    }

    @Test
    void choicesWithQuotesInsideStillCount() throws Exception {
        // formatOptions escape nháy bên trong thành \" — vẫn là 2 phần tử.
        assertThat(hasRealChoices("[\"say \\\"hi\\\"\", \"say \\\"bye\\\"\"]")).isTrue();
    }

    // ---------- Phải TỪ CHỐI ----------

    @Test
    void nullIsNotReal() throws Exception {
        assertThat(hasRealChoices(null)).isFalse();
    }

    @Test
    void blankIsNotReal() throws Exception {
        assertThat(hasRealChoices("   ")).isFalse();
    }

    @Test
    void emptyArrayIsNotReal() throws Exception {
        assertThat(hasRealChoices("[]")).isFalse();
    }

    @Test
    void singleChoiceIsNotReal() throws Exception {
        // Một lựa chọn không phải bài trắc nghiệm — không đủ để hạ nhãn.
        assertThat(hasRealChoices("[\"only one\"]")).isFalse();
    }

    @Test
    void plainTextIsNotReal() throws Exception {
        // Đây là hình dạng của row 745673: options là văn bản, không phải mảng.
        assertThat(hasRealChoices("These Terms are for the guidance of our Service.")).isFalse();
    }

    @Test
    void letterPrefixedTextIsNotReal() throws Exception {
        // Đây là hình dạng của row 745808: "A) ...B) ..." chứ không phải mảng JSON.
        assertThat(hasRealChoices("\"A) The weather in a specific location.B) A family's routine activities.\"")).isFalse();
    }

    @Test
    void jsonObjectIsNotReal() throws Exception {
        assertThat(hasRealChoices("{\"a\": \"b\"}")).isFalse();
    }

    @Test
    void unbalancedBracketsAreNotReal() throws Exception {
        assertThat(hasRealChoices("[\"a\", \"b\"")).isFalse();
    }

    @Test
    void nullLiteralIsNotReal() throws Exception {
        assertThat(hasRealChoices("null")).isFalse();
    }

    // ---------- Đối chiếu với dữ liệu THẬT trong DB ----------

    @Test
    void realDbShapeOfTheSevenRowsIsAccepted() throws Exception {
        // Nguyên văn options của exercise 745713 — một trong 7 row đã được hạ nhãn.
        String real = "[\"A. The man is a doctor.\",\"B. The woman is a teacher.\","
                + "\"C. The man is a lawyer.\",\"D. The woman is a nurse.\"]";
        assertThat(hasRealChoices(real)).isTrue();
    }

    @Test
    void realDbShapeOfTheTwoRemainingRowsIsRejected() throws Exception {
        // Nguyên văn options của 745673 và 745808 — 2 row CỐ Ý không hạ nhãn,
        // vì chúng cần quyết định nội dung chứ không sửa tự động được.
        assertThat(hasRealChoices("These Terms are for the guidance of our Service. "
                + "These Terms are for the guidance of our Service.")).isFalse();
        assertThat(hasRealChoices("\"A) The weather in a specific location."
                + "B) A family's routine activities."
                + "C) The location of a favorite restaurant."
                + "D) A university accommodation.\"")).isFalse();
    }
}
