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

@Service
@Slf4j
/**
 * class DuckDuckGoResearchService.
 */
public class DuckDuckGoResearchService {

    private final boolean enabled;

    public DuckDuckGoResearchService(@Value("${ai.exercise.duckduckgo.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public static class SearchResult {
        public String title;
        public String snippet;
        public String url;
    }

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
