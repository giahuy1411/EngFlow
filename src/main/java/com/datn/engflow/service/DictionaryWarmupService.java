package com.datn.engflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * audit-v17 remove-limits round (L1-B) — pre-warm the common-word list into the dictionary cache.
 *
 * <p>WHY. The dictionary upstream (dictionaryapi.dev) has a measured server-side TTFB of ~19.5 s —
 * the delay is on their side, not the VN network (DNS + connect + TLS together measured 0.17 s).
 * A cold lookup therefore costs ~20 s for every user. Warming the words people actually look up
 * turns that into an instant Redis hit for everyone afterwards.
 *
 * <p>HOW. On {@link ApplicationReadyEvent} (and nightly) this reads
 * {@code classpath:dictionary/common-words.txt} and calls {@link DictionaryService#lookup(String)}
 * once per word, which writes the result into the {@code dictionary} cache. It runs on a background
 * thread so startup is never blocked, and it is SERIAL and BOUNDED:
 * <ul>
 *   <li>Serial — the upstream throttles concurrent requests. Measured: 6 parallel lookups produced
 *       5 timeouts and returned {@code []}; serial lookups succeed.</li>
 *   <li>Bounded — at most {@code dictionary.warmup.limit} words per run.</li>
 *   <li>Abort-on-failure — if a lookup returns {@code "[]"} (upstream down / throttling), the run
 *       STOPS rather than hammering the upstream for the remaining words. A later run resumes.</li>
 * </ul>
 *
 * <p>Disable with {@code dictionary.warmup.enabled=false} (tests set this so no network is touched).
 * The whole run is best-effort: any exception is logged and swallowed — a failed warm-up must never
 * affect application health.
 */
@Component
@Slf4j
public class DictionaryWarmupService {

    private static final String WORD_LIST = "dictionary/common-words.txt";

    /**
     * Stop the run only after this many empty responses IN A ROW — the signal that the upstream is
     * down, not that one word is missing. Chosen from the measured behaviour: single function words
     * ("of") are routinely unserved, but a genuinely down upstream fails every word.
     */
    static final int MAX_CONSECUTIVE_FAILURES = 5;

    private final DictionaryService dictionaryService;

    @Value("${dictionary.warmup.enabled:true}")
    private boolean enabled;

    @Value("${dictionary.warmup.limit:500}")
    private int limit;

    public DictionaryWarmupService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    /** One warm-up shortly after startup, on a background thread (never blocks readiness). */
    @EventListener(ApplicationReadyEvent.class)
    public void warmOnStartup() {
        if (!enabled) {
            return;
        }
        Thread t = new Thread(this::runWarmup, "dictionary-warmup-startup");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Nightly refresh so entries whose 1 h TTL lapsed are re-populated before the day's traffic.
     *
     * <p>Runs on its OWN daemon thread, NOT on the scheduler thread. The serial loop takes up to
     * {@code limit} × ~20 s (measured) — ~67 min for the default 200 — and Spring's default
     * scheduler pool is a single thread, so running it inline would block every other
     * {@code @Scheduled} job (notably {@code SePayPollingScheduler}, whose 60 s fixedDelay would be
     * starved for the whole window and manual transfers would stop being auto-detected).
     */
    @Scheduled(cron = "${dictionary.warmup.cron:0 30 3 * * *}")
    public void warmNightly() {
        if (!enabled) {
            return;
        }
        Thread t = new Thread(this::runWarmup, "dictionary-warmup-nightly");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Read the word list and warm each word, serially.
     *
     * <p>Failure policy — learned by running it: aborting on the FIRST empty response is too
     * brittle. The list contains function words ("of", "the", "and") that dictionaryapi.dev does
     * not serve, and the upstream also returns HTTP 522 under load; both yield an empty body. An
     * abort-on-first policy stopped the whole warm-up at word 1. So instead: a single empty
     * response is SKIPPED (that word is simply not warm-able), and the run only stops after
     * {@link #MAX_CONSECUTIVE_FAILURES} in a row — which is the real signal that the upstream is
     * down rather than that one word is missing.
     *
     * @return the number of words successfully warmed (also logged)
     */
    public int runWarmup() {
        List<String> words;
        try {
            words = readWords();
        } catch (Exception e) {
            log.warn("Dictionary warm-up: could not read {}: {}", WORD_LIST, e.getMessage());
            return 0;
        }
        int cap = Math.min(words.size(), Math.max(0, limit));
        log.info("Dictionary warm-up starting: {} words (cap {})", words.size(), cap);

        int warmed = 0;
        int skipped = 0;
        int consecutiveFailures = 0;
        for (int i = 0; i < cap; i++) {
            String word = words.get(i);
            String json;
            try {
                json = dictionaryService.lookup(word);
            } catch (Exception e) {
                json = null;
            }
            if (json == null || json.isBlank() || "[]".equals(json)) {
                skipped++;
                consecutiveFailures++;
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    log.warn("Dictionary warm-up aborted at '{}' ({}/{}): {} consecutive empty responses — upstream looks down",
                            word, i, cap, consecutiveFailures);
                    break;
                }
                continue;
            }
            warmed++;
            consecutiveFailures = 0;
            if (warmed % 25 == 0) {
                log.info("Dictionary warm-up progress: {}/{} (skipped {})", warmed, cap, skipped);
            }
        }
        log.info("Dictionary warm-up finished: {} words warmed, {} skipped", warmed, skipped);
        return warmed;
    }

    /** Parse the classpath word list: one word per line, '#' comments and blanks ignored. */
    List<String> readWords() throws Exception {
        List<String> words = new ArrayList<>();
        ClassPathResource res = new ClassPathResource(WORD_LIST);
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String w = line.trim().toLowerCase();
                if (w.isEmpty() || w.startsWith("#")) {
                    continue;
                }
                words.add(w);
            }
        }
        return words;
    }
}
