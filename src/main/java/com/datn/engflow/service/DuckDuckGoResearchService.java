package com.datn.engflow.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kênh nghiên cứu web tuỳ chọn cho việc sinh bài tập: bóc trang HTML kết quả của
 * DuckDuckGo rồi trả về tiêu đề, snippet và URL, để {@link AiExerciseService}
 * ghép vào prompt sinh bài như ngữ cảnh bổ sung.
 *
 * <p>Hoàn toàn tuỳ chọn — cả lớp suy giảm thành "không có kết quả" khi cờ tắt
 * hoặc request lỗi, nên việc sinh bài không bao giờ phụ thuộc vào nó. Mọi lỗi ở
 * đây đều phải bị nuốt: research chỉ là phần phụ, hỏng nó không được làm hỏng
 * luồng chính.</p>
 */
@Service
@Slf4j
public class DuckDuckGoResearchService {

    private final boolean enabled;

    /**
     * @param enabled bật/tắt research, lấy từ
     *               {@code ai.exercise.duckduckgo.enabled}
     */
    public DuckDuckGoResearchService(@Value("${ai.exercise.duckduckgo.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Một kết quả tìm kiếm bóc được.
     */
    public static class SearchResult {
        public String title;
        public String snippet;
        public String url;
    }

    /**
     * Bóc trang kết quả HTML của DuckDuckGo cho {@code query}. Kết quả không có
     * tiêu đề bị loại. Hỏng thì im lặng chịu: cờ tắt, lỗi mạng hay markup đổi
     * đều cho ra danh sách rỗng chứ không ném ngoại lệ, vì research chỉ là phần
     * bổ trợ cho việc sinh bài.
     *
     * @param query      cụm từ tìm kiếm
     * @param maxResults số kết quả trả về nhiều nhất
     * @return các kết quả tìm được, có thể rỗng
     */
    public List<SearchResult> search(String query, int maxResults) {
        if (!enabled) {
            log.info("DuckDuckGo research disabled, returning empty results");
            return List.of();
        }
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) EngFlow/1.0")
                    .timeout(10000)
                    .get();

            Elements results = doc.select(".result");
            List<SearchResult> searchResults = new ArrayList<>();
            for (Element result : results) {
                if (searchResults.size() >= maxResults) break;
                SearchResult sr = new SearchResult();
                Element titleEl = result.selectFirst(".result__title a, .result__a");
                Element snippetEl = result.selectFirst(".result__snippet");
                if (titleEl != null) {
                    sr.title = titleEl.text();
                    sr.url = titleEl.absUrl("href");
                }
                if (snippetEl != null) {
                    sr.snippet = snippetEl.text();
                }
                if (sr.title != null && !sr.title.isBlank()) {
                    searchResults.add(sr);
                }
            }
            log.info("DuckDuckGo search '{}' returned {} results", query, searchResults.size());
            return searchResults;
        } catch (Exception e) {
            log.warn("DuckDuckGo search failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * Kết xuất các kết quả tìm kiếm cho một chủ đề bài học thành danh sách gạch
     * đầu dòng sẵn sàng đưa vào prompt. Query được thêm đuôi bằng từ ngữ
     * "English lesson" vì endpoint được tinh chỉnh cho mảng đó, và một chủ đề
     * trần trụi trả về rất ít tài liệu dùng được.
     *
     * @param topic      tiêu đề hoặc chủ đề bài học
     * @param maxSources số nguồn tối đa đưa vào
     * @return khối "- tiêu đề: snippet" phân tách bằng xuống dòng, hoặc chuỗi rỗng
     *         khi tìm kiếm không ra gì
     */
    public String researchLessonTopic(String topic, int maxSources) {
        List<SearchResult> results = search(topic + " English lesson grammar exercises", maxSources);
        if (results.isEmpty()) {
            return "";
        }
        return results.stream()
                .map(r -> "- " + r.title + ": " + (r.snippet != null ? r.snippet : ""))
                .collect(Collectors.joining("\n"));
    }
}
