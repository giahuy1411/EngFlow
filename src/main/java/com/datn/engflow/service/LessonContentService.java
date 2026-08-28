package com.datn.engflow.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

@Slf4j
@Service
/**
 * class LessonContentService.
 */
public class LessonContentService {

    private static final String[] DIVI_WRAPPERS = {
        "div.et_pb_text_inner",
        "div.et_pb_text",
        "div.et_pb_column",
        "div.et_pb_row",
        "div.et_pb_section",
        "div.et_block_text",
        "div.et_block_column",
        "div.et_block_row",
        "div.et_block_section",
    };

    /** Selectors for ALL Divi/WordPress wrapper elements to unwrap. */
    private static final String[] DEEP_CLEAN_UNWRAP_SELECTORS = {
        "[class*=et_pb_text_inner]",
        "[class*=et_pb_text]",
        "[class*=et_pb_module]",
        "[class*=et_pb_column]",
        "[class*=et_pb_row]",
        "[class*=et_pb_section]",
        "[class*=et_block_]",
        "[class*=et_pb_bg_layout]",
        "[class*=et_pb_css_mix_blend]",
        "div.et_builder_inner_content",
        "div.et-l",
        "div.entry-content",
        "div.et_post_meta_wrapper",
        "div.skill-html",
    };

    private static final Pattern NBSP_PATTERN = Pattern.compile("(\\s*&nbsp;\\s*){2,}");
    private static final Pattern MULTI_SPACE = Pattern.compile(" {3,}");

    /** Matches escaped HTML ad tags appearing as visible text. */
    private static final Pattern ESCAPED_AD_TEXT = Pattern.compile(
        "</?iframe[^>]*>|</?ins[^>]*>|</?script[^>]*>|google_ads_frame\\w*|" +
        "adsbygoogle|pagead2|googlesyndication|ca-pub-\\d+",
        Pattern.CASE_INSENSITIVE
    );

    /** Common mojibake patterns from UTF-8 data misread as Windows-1252. */
    private static final String[][] MOJIBAKE_REPLACEMENTS = {
        {"\u00e2\u0080\u0099", "'"},
        {"\u00e2\u0080\u0098", "'"},
        {"\u00e2\u0080\u009c", "\""},
        {"\u00e2\u0080\u009d", "\""},
        {"\u00e2\u0080\u0093", "\u2013"},
        {"\u00e2\u0080\u0094", "\u2014"},
        {"\u00e2\u0080\u00a6", "\u2026"},
        {"\u00e2\u0080\u00b2", "'"},
        {"\u00c3\u00a9", "\u00e9"},
    };
    private static final Pattern REPLACEMENT_CHARS = Pattern.compile("[\uFFFD]{2,}");
    private static final Pattern REPLACEMENT_S = Pattern.compile("[\uFFFD]\\??s");

    /**
     * Deep clean scraped HTML: strip ALL Divi wrappers, inline styles, ads, scripts,
     * WordPress audio player wrappers, and normalize to clean semantic HTML.
     * Converts Answer toggle sections to native HTML5 {@code <details>} elements.
     *
     * @param rawHtml the original scraped HTML content
     * @return cleaned semantic HTML
     */
    public String deepCleanHtml(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) return rawHtml;

        Document doc = Jsoup.parse(rawHtml);
        doc.outputSettings().indentAmount(0).outline(false);

        // 1. Remove scripts, styles, noscript
        doc.select("script, style, noscript").remove();

        // 2. Remove ads and ALL iframes (lesson content should never need iframes)
        doc.select("ins.adsbygoogle, ins[class*=ads]").remove();
        doc.select("[id*=advert], [class*=advert], [class*=ad-], [id*=ad-]").remove();
        doc.select("iframe").remove();

        // 2b. Remove escaped HTML ad fragments appearing as visible text
        removeEscapedAdText(doc);

        // 2c. Fix mojibake/encoding artifacts
        fixEncodingArtifacts(doc);

        // 3. Remove social share buttons
        doc.select(".et_social_networks, .et_social_share, .shared-counts, .share-this").remove();

        // 4. Remove Related Posts sections
        doc.select("[class*=rpbt], .rpbt_shortcode").remove();
        for (Element h : doc.select("h3:containsOwn(RELATED POSTS), h3:containsOwn(Related Posts)")) {
            Element section = h.closest("[class*=et_pb_section]");
            if (section != null) {
                section.remove();
            } else {
                h.remove();
            }
        }

        // 5. Remove the duplicate h1 title (already stored in lesson.title column)
        doc.select("h1.entry-title, h1:first-of-type").remove();

        // 6. Convert Answer toggle sections to native <details><summary>
        convertAnswerToggles(doc);

        // 7. Clean WordPress audio players to native <audio> tags
        cleanAudioPlayers(doc);

        // 8. Remove ALL inline style attributes
        for (Element el : doc.select("[style]")) {
            el.removeAttr("style");
        }

        // 9. Unwrap meaningless <span> tags (keep content)
        for (Element span : doc.select("span")) {
            if (span.attributes().size() == 0 ||
                (span.attributes().size() == 1 && span.hasAttr("class") && span.className().isBlank())) {
                span.unwrap();
            }
        }

        // 10. Unwrap ALL Divi/WordPress container divs (inside-out order)
        for (String selector : DEEP_CLEAN_UNWRAP_SELECTORS) {
            for (Element el : doc.select(selector)) {
                el.unwrap();
            }
        }

        // 11. Remove remaining wrapper divs that have no semantic value
        removeEmptyWrapperDivs(doc);

        // 12. Normalize tables: remove inline width, height attributes
        for (Element el : doc.select("table, td, th, tr, col, colgroup")) {
            el.removeAttr("width");
            el.removeAttr("height");
            el.removeAttr("border");
            el.removeAttr("cellspacing");
            el.removeAttr("cellpadding");
        }

        // 13. Remove all class, id, and ad-related attributes
        for (Element el : doc.getAllElements()) {
            el.removeAttr("class");
            el.removeAttr("id");
            List<String> toRemove = new ArrayList<>();
            for (Attribute attr : el.attributes()) {
                String key = attr.getKey();
                if (key.startsWith("data-ad") || key.startsWith("data-google") ||
                    key.equals("role") || key.equals("tabindex") || key.equals("aria-label") ||
                    key.equals("aria-controls") || key.equals("aria-valuemin") ||
                    key.equals("aria-valuemax") || key.equals("aria-valuenow") ||
                    key.equals("aria-valuetext") || key.equals("aria-live") ||
                    key.equals("title") || key.equals("crossorigin") || key.equals("async")) {
                    toRemove.add(key);
                }
            }
            toRemove.forEach(el::removeAttr);
        }

        // 14. Remove empty elements
        removeEmptyElements(doc);

        // 15. Normalize &nbsp;
        String html = doc.body() != null ? doc.body().html() : doc.html();
        html = NBSP_PATTERN.matcher(html).replaceAll(" ");
        html = MULTI_SPACE.matcher(html).replaceAll(" ");
        html = html.replace("&nbsp;", " ");

        // 15b. Final escaped text cleanup on the serialized HTML string
        html = ESCAPED_AD_TEXT.matcher(html).replaceAll("");

        // 16. Final pass to clean up any remaining empty tags
        Document finalDoc = Jsoup.parseBodyFragment(html);
        removeEmptyElements(finalDoc);

        Element body = finalDoc.body();
        return body != null ? body.html().strip() : html.strip();
    }

    /**
     * Convert Divi Answer toggle sections to native HTML5 details/summary elements.
     * Also handles standalone h5 "Answer" headings followed by content.
     */
    private void convertAnswerToggles(Document doc) {
        // Handle Divi toggle containers with class-based selectors
        Elements toggles = doc.select("[class*=et_pb_toggle]");
        for (Element toggle : toggles) {
            Element titleEl = toggle.selectFirst("[class*=et_pb_toggle_title]");
            Element contentEl = toggle.selectFirst("[class*=et_pb_toggle_content]");

            if (contentEl != null) {
                String title = (titleEl != null) ? titleEl.text() : "ANSWER";
                String innerHtml = contentEl.html();

                Element details = doc.createElement("details");
                Element summary = details.appendElement("summary");
                summary.text(title);
                details.append(innerHtml);

                toggle.replaceWith(details);
            }
        }

        // Also handle standalone h5 "Answer" headings followed by content
        // (some pages use <h5>Answer</h5> instead of Divi toggle)
        for (Element h5 : doc.select("h5:containsOwn(Answer)")) {
            // Skip if already inside a <details>
            if (h5.closest("details") != null) continue;

            // Collect sibling content until next heading or toggle
            StringBuilder answerHtml = new StringBuilder();
            List<Node> toRemove = new ArrayList<>();
            Node sibling = h5.nextSibling();
            while (sibling != null) {
                if (sibling instanceof Element el) {
                    String tag = el.tagName();
                    if (tag.matches("h[1-5]") || el.className().contains("et_pb_toggle")) break;
                }
                if (sibling instanceof Element el) {
                    answerHtml.append(el.outerHtml());
                } else if (sibling instanceof TextNode tn) {
                    answerHtml.append(tn.getWholeText());
                }
                toRemove.add(sibling);
                sibling = sibling.nextSibling();
            }

            if (!answerHtml.isEmpty()) {
                Element details = doc.createElement("details");
                Element summary = details.appendElement("summary");
                summary.text("ANSWER");
                details.append(answerHtml.toString());

                h5.replaceWith(details);
                toRemove.forEach(Node::remove);
            }
        }
    }

    /**
     * Extract native audio tags from WordPress MediaElement.js wrappers.
     */
    private void cleanAudioPlayers(Document doc) {
        Elements audioWrappers = doc.select(
            "[class*=et_pb_audio_module], [class*=mejs-container], [class*=et_pb_audio]"
        );

        for (Element wrapper : audioWrappers) {
            // If this wrapper was already removed (e.g. it was a child of an outer wrapper that got replaced), skip it
            if (wrapper.parent() == null || wrapper.closest("body") == null) {
                continue;
            }

            Element audioTag = wrapper.selectFirst("audio[src], audio source[src]");
            if (audioTag != null) {
                String src;
                if (audioTag.tagName().equals("audio") && audioTag.hasAttr("src")) {
                    src = audioTag.attr("src");
                } else if (audioTag.tagName().equals("source")) {
                    src = audioTag.attr("src");
                } else {
                    continue;
                }

                if (src.contains("?_=")) {
                    src = src.substring(0, src.indexOf("?_="));
                }

                Element cleanAudio = doc.createElement("audio");
                cleanAudio.attr("controls", "");
                cleanAudio.attr("src", src);

                Element outerWrapper = wrapper;
                Element parent = wrapper.parent();
                while (parent != null && parent.classNames().stream()
                        .anyMatch(c -> c.contains("et_pb_audio") || c.contains("et_audio"))) {
                    outerWrapper = parent;
                    parent = parent.parent();
                }
                outerWrapper.replaceWith(cleanAudio);
            }
        }

        for (Element mew : doc.select("mediaelementwrapper")) {
            Element audio = mew.selectFirst("audio");
            if (audio != null) {
                mew.replaceWith(audio);
            }
        }

        // Remove any remaining media wrappers that failed to convert (e.g. no valid src).
        // This prevents their internal screen-reader texts ("Use Up/Down Arrow keys", "00:00")
        // from leaking out as visible text when classes are stripped.
        doc.select("[class*=mejs-container], [class*=et_pb_audio], .mejs-offscreen, [class*=mejs-time], .mejs-controls").remove();
    }

    /**
     * Remove empty elements recursively.
     */
    private void removeEmptyElements(Document doc) {
        boolean changed = true;
        int maxPasses = 5;
        while (changed && maxPasses-- > 0) {
            changed = false;
            for (Element el : doc.select("p, div, span, strong, em, b, i")) {
                if (el.childNodeSize() == 0 || el.html().isBlank()) {
                    el.remove();
                    changed = true;
                }
            }
        }
    }

    /**
     * Remove remaining wrapper divs that have no semantic value.
     */
    private void removeEmptyWrapperDivs(Document doc) {
        boolean changed = true;
        int maxPasses = 10;
        while (changed && maxPasses-- > 0) {
            changed = false;
            for (Element div : doc.select("div")) {
                boolean hasOnlyEmptyAttrs = div.attributes().size() == 0 ||
                    (div.attributes().size() == 1 && div.hasAttr("class") && div.className().isBlank());
                if (hasOnlyEmptyAttrs) {
                    div.unwrap();
                    changed = true;
                }
            }
        }
    }

    /**
     * Remove text nodes and elements containing escaped HTML ad fragments that appear
     * as visible text after sanitization (e.g. the literal text "&lt;iframe id='google_ads_frame0'&gt;").
     */
    private void removeEscapedAdText(Document doc) {
        for (Element el : doc.select("p, div, span")) {
            String text = el.ownText();
            if (ESCAPED_AD_TEXT.matcher(text).find()) {
                // If the element only contains ad text (no meaningful children), remove entirely
                if (el.children().isEmpty() || el.text().strip().length() < 200) {
                    el.remove();
                }
            }
        }
    }

    /**
     * Fix mojibake/encoding artifacts from UTF-8/Windows-1252 confusion.
     * Also cleans sequences of Unicode replacement characters (U+FFFD).
     */
    private void fixEncodingArtifacts(Document doc) {
        Element body = doc.body();
        if (body == null) return;
        String html = body.html();

        for (String[] pair : MOJIBAKE_REPLACEMENTS) {
            html = html.replace(pair[0], pair[1]);
        }

        // Replace sequences of replacement characters with ellipsis
        html = REPLACEMENT_S.matcher(html).replaceAll("'s");
        html = REPLACEMENT_CHARS.matcher(html).replaceAll("\u2026");
        // Remove single remaining replacement characters
        html = html.replace("\uFFFD", "");

        body.html(html);
    }

    /**
     * Strip Answer sections, Related Posts, ads, scripts, empty wrappers from lesson HTML.
     * Unwraps unnecessary Divi container divs so content renders cleanly.
     */
    public String stripAnswersAndJunk(String rawHtml) {
        if (rawHtml == null || rawHtml.isEmpty()) return rawHtml;
        Document doc = Jsoup.parse(rawHtml);

        Elements answerToggles = doc.select(".et_pb_toggle:has(.et_pb_toggle_title:contains(Answer))");
        answerToggles.remove();

        Elements answerHeadings = doc.select("h5:containsOwn(Answer)");
        answerHeadings.remove();

        doc.select(".et_pb_section:has(h3:containsOwn(RELATED))").remove();
        doc.select(".rpbt_shortcode").remove();
        Elements relatedHeadings = doc.select("h3:containsOwn(RELATED POSTS)");
        if (!relatedHeadings.isEmpty()) {
            Element parent = relatedHeadings.first().parent();
            if (parent != null) {
                Element section = parent.closest(".et_pb_section");
                if (section != null) {
                    section.remove();
                } else {
                    relatedHeadings.first().remove();
                }
            }
        }

        doc.select("ins.adsbygoogle").remove();
        doc.select("[id*=advert], [class*=advert], [class*=ad-], [id*=ad-]").remove();
        doc.select("iframe[src*=ads]").remove();

        doc.select("script, style, noscript").remove();

        doc.select(".et_social_networks, .et_social_share, .shared-counts, .share-this").remove();

        Elements paragraphs = doc.select("p");
        for (Element p : paragraphs) {
            if (p.children().isEmpty() && p.textNodes().isEmpty()) {
                p.remove();
            }
        }

        for (String selector : DIVI_WRAPPERS) {
            Elements wrappers = doc.select(selector);
            for (Element el : wrappers) {
                if (el.attributes().size() <= 1) {
                    el.unwrap();
                }
            }
        }

        Elements emptyDivs = doc.select("div");
        for (Element div : emptyDivs) {
            if (div.textNodes().isEmpty() && div.children().isEmpty()) {
                div.remove();
            }
        }

        Element body = doc.body();
        if (body != null) {
            return body.html();
        }
        return doc.html();
    }

    /**
     * Strip only ads, scripts, social junk — keep everything else (including answers).
     */
    public String stripJunkOnly(String rawHtml) {
        if (rawHtml == null || rawHtml.isEmpty()) return rawHtml;
        Document doc = Jsoup.parse(rawHtml);
        doc.select("ins.adsbygoogle").remove();
        doc.select("script, style, noscript").remove();
        doc.select(".et_social_networks, .et_social_share").remove();
        Element body = doc.body();
        return body != null ? body.html() : doc.html();
    }

/**
 * record LessonContentInfo.
 */
    public record LessonContentInfo(String cleanHtml) {}
}
