package com.datn.engflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nạp bộ đáp án của corpus {@code tienganh_nangcao_lessons.json} vào bộ nhớ lúc
 * khởi động và tra cứu theo bài học (unit) cùng mã kỹ năng ("vcb", "gra", "lis",
 * "rea").
 *
 * <p>Vì sao phải bóc bằng regex thay vì đóng gói thành dữ liệu: file nguồn chỉ
 * chứa đáp án dưới dạng chữ hiển thị nằm trong HTML của từng kỹ năng, không có
 * trường JSON riêng. Đây là dịch vụ hỗ trợ cho luồng backfill đáp án bằng AI —
 * chế độ deterministic lấy đáp án từ các khoá ANSWER trong thẻ summary — và chỉ
 * được đọc bởi {@code JsonDataSeeder}. Corpus thiếu không phải lỗi khởi động:
 * nó chỉ có nghĩa là không có đáp án nào để dùng.</p>
 */
@Service
public class AnswerKeyService {

    private static final Logger log = LoggerFactory.getLogger(AnswerKeyService.class);
    private final Map<String, Map<Long, Map<Integer, String>>> keys = new HashMap<>();
    private static final Pattern ANSWER_PATTERN = Pattern.compile("result(\\d+)\\s*=\\s*['\"]?([^'\";,]+?)['\"]?\\s*[;,]");
    private static final Pattern ARR_RESULT_PATTERN = Pattern.compile("arr_result\\[\\d+]\\[\\d+]\\s*=\\s*['\"]?([^'\";]+?)['\"]?\\s*[;,]");

    /**
     * Hook vòng đời của Spring: nạp corpus đúng một lần, sau khi bean được tạo
     * nhưng trước khi nó phục vụ bất kỳ request nào.
     */
    @PostConstruct
    public void init() {
        loadFromJson();
    }

    /**
     * Đọc file JSON corpus và đổ vào {@link #keys}. Thử filesystem trước rồi mới
     * tới classpath, để bản checkout khi phát triển có thể ghi đè bản đóng gói
     * mà không cần build lại. Không bao giờ ném ngoại lệ: corpus thiếu hoặc hỏng
     * chỉ được ghi log, dịch vụ tiếp tục trả về map rỗng.
     */
    private void loadFromJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = null;
            String[] locations = {
                "tienganh_nangcao_lessons.json",
                "classpath:tienganh_nangcao_lessons.json"
            };
            for (String loc : locations) {
                try {
                    if (loc.startsWith("classpath:")) {
                        is = getClass().getClassLoader().getResourceAsStream(loc.substring(10));
                    } else {
                        is = new FileInputStream(loc);
                    }
                    if (is != null) break;
                } catch (Exception e) {
                    continue;
                }
            }
            if (is == null) {
                log.warn("AnswerKeyService: Could not find tienganh_nangcao_lessons.json");
                return;
            }

            JsonNode root = mapper.readTree(is);
            JsonNode units = root.get("units");
            if (units == null || !units.isArray()) return;

            String[] skillTypes = {"vocabulary", "grammar", "listening", "reading"};
            String[] skillCodes = {"vcb", "gra", "lis", "rea"};

            for (JsonNode unit : units) {
                long lessonId = unit.get("unit").asLong();
                JsonNode skills = unit.get("skills");
                if (skills == null) continue;

                for (int si = 0; si < skillTypes.length; si++) {
                    JsonNode skill = skills.get(skillTypes[si]);
                    if (skill == null) continue;
                    String content = skill.has("content") ? skill.get("content").asText() : "";
                    Map<Integer, String> answers = parseAnswers(content);
                    if (!answers.isEmpty()) {
                        keys.computeIfAbsent(skillCodes[si], k -> new HashMap<>())
                            .put(lessonId, answers);
                    }
                }
            }
            log.info("AnswerKeyService: Loaded answer keys for {} quizzes", countKeys());
        } catch (Exception e) {
            log.error("AnswerKeyService init error: {}", e.getMessage(), e);
        }
    }

    /**
     * Bóc map đáp án đã đánh số từ HTML nội dung của một kỹ năng. Corpus có hai
     * dạng: gán {@code resultN = 'x';} và các phần tử {@code arr_result[i][j] = 'x';}.
     *
     * <p>Với dạng {@code arr_result}, các hàng được đánh số theo thứ tự bắt gặp
     * chứ không theo chỉ số {@code [i][j]}, vì chỉ số đó không khớp với số thứ tự
     * câu hỏi mà người học nhìn thấy. Đánh số theo chỉ số sẽ lệch đáp án.</p>
     *
     * @param html HTML nội dung của kỹ năng
     * @return map số câu sang nội dung đáp án, có thể rỗng
     */
    private Map<Integer, String> parseAnswers(String html) {
        Map<Integer, String> answers = new HashMap<>();
        if (html == null || html.isEmpty()) return answers;
        Matcher m = ANSWER_PATTERN.matcher(html);
        while (m.find()) {
            int qIdx = Integer.parseInt(m.group(1));
            String ans = m.group(2).trim();
            answers.put(qIdx, ans);
        }
        Matcher am = ARR_RESULT_PATTERN.matcher(html);
        while (am.find()) {
            String ans = am.group(1).trim();
            answers.put(answers.size(), ans);
        }
        return answers;
    }

    /**
     * Đọc lại corpus vào bộ nhớ, thay thế toàn bộ nội dung đã nạp trước đó. Được
     * mở ra để một instance chạy lâu có thể nhận file corpus vừa sửa mà không
     * phải khởi động lại.
     */
    public void loadAllAnswerKeys() {
        loadFromJson();
    }

    /**
     * Tra đáp án của một bài học theo một kỹ năng.
     *
     * @param lessonId  id bài học (unit)
     * @param skillType mã kỹ năng: "vcb", "gra", "lis" hoặc "rea"
     * @return map số câu sang nội dung đáp án; rỗng khi không có, không bao giờ null
     */
    public Map<Integer, String> getAnswers(long lessonId, String skillType) {
        Map<Long, Map<Integer, String>> byLesson = keys.get(skillType);
        if (byLesson == null) return Collections.emptyMap();
        return byLesson.getOrDefault(lessonId, Collections.emptyMap());
    }

    /**
     * @param lessonId  id bài học (unit)
     * @param skillType mã kỹ năng: "vcb", "gra", "lis" hoặc "rea"
     * @return true khi cặp (bài học, kỹ năng) này có ít nhất một đáp án
     */
    public boolean hasAnswers(long lessonId, String skillType) {
        return !getAnswers(lessonId, skillType).isEmpty();
    }

    /**
     * @return tổng số đáp án riêng lẻ trên mọi kỹ năng đã nạp, chỉ dùng cho dòng
     *         log lúc khởi động
     */
    private int countKeys() {
        return keys.values().stream()
            .mapToInt(m -> m.values().stream().mapToInt(Map::size).sum())
            .sum();
    }
}
