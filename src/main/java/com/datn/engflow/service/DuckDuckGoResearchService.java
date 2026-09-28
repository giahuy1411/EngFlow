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
 * Optional web-research side-channel for exercise generation: scrapes the
 * DuckDuckGo HTML endpoint and hands back titles, snippets and URLs, which
 * {@link AiExerciseService} folds into its generation prompt as extra context.
 * Entirely optional — the whole class degrades to "no results" when the flag
 * is off or the request fails, so generation never depends on it.
 */
public class DuckDuckGoResearchService {

    private final boolean enabled;

    /**
     * @param enabled research on/off, from
     *               {@code ai.exercise.duckduckgo.enabled}
     */
    public DuckDuckGoResearchService(@Value("${ai.exercise.duckduckgo.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * One scraped search hit.
     */
    public static class SearchResult {
        public String title;
        public String snippet;
        public String url;
    }

    /**
     * Scrapes the DuckDuckGo HTML results page for {@code query}. Results with
     * no title are dropped. Fails soft — a disabled flag, a network error or a
     * markup change all yield an empty list rather than an exception, because
     * research is supplementary to generation.
     *
     * @param query      the search phrase
     * @param maxResults the most results to return
     * @return the hits found, possibly empty
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
     * Renders search hits for a lesson topic as a prompt-ready bullet list.
     * The query is padded with English-lesson wording because the endpoint is
     * tuned for that and a bare topic returns little usable material.
     *
     * @param topic      the lesson title or topic
     * @param maxSources the most sources to include
     * @return a newline-separated "- title: snippet" block, or an empty string
     *         when the search returns nothing
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
