package com.datn.engflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * audit-v17 remove-limits round (L1-B) — dictionary warm-up.
 *
 * <p>These tests never touch the network: {@link DictionaryService} is mocked. They pin the three
 * load-bearing properties of the warm-up — it reads the classpath list, it is SERIAL, and it STOPS
 * at the first empty upstream response (so a failing/throttling upstream is not hammered).
 */
class DictionaryWarmupServiceTest {

    private DictionaryService dictionaryService;
    private DictionaryWarmupService warmup;

    @BeforeEach
    void setUp() {
        dictionaryService = mock(DictionaryService.class);
        warmup = new DictionaryWarmupService(dictionaryService);
        ReflectionTestUtils.setField(warmup, "enabled", true);
        ReflectionTestUtils.setField(warmup, "limit", 500);
    }

    @Test
    void readWords_loadsClasspathList_skippingCommentsAndBlanks() throws Exception {
        List<String> words = warmup.readWords();
        assertThat(words).isNotEmpty();
        assertThat(words).doesNotContain("#");
        assertThat(words).allSatisfy(w -> assertThat(w).isEqualTo(w.toLowerCase()).isNotBlank());
        // The list ships in the jar; it must be a meaningful size, not an empty/placeholder file.
        assertThat(words.size()).isGreaterThan(100);
    }

    @Test
    void readWords_isTheNgslList_cleanAndDeduplicated() throws Exception {
        // audit-v17 follow-up: the hand-entered list was replaced by the NGSL 1.2 (2,809 words,
        // CC BY-SA 4.0 — see the file header + README). Pin its shape so a bad regeneration fails.
        List<String> words = warmup.readWords();
        assertThat(words).hasSize(2809);
        assertThat(words).doesNotHaveDuplicates();
        // Every entry is a plain lower-case token (no spaces, digits, or stray punctuation).
        assertThat(words).allSatisfy(w -> assertThat(w).matches("[a-z'-]+"));
        // Frequency-ordered: the NGSL's most frequent word is "the".
        assertThat(words.get(0)).isEqualTo("the");
    }

    @Test
    void runWarmup_callsLookupOncePerWord_serially_upToTheCap() throws Exception {
        ReflectionTestUtils.setField(warmup, "limit", 5);
        when(dictionaryService.lookup(anyString())).thenReturn("[{\"word\":\"x\"}]");

        int warmed = warmup.runWarmup();

        assertThat(warmed).isEqualTo(5);
        verify(dictionaryService, times(5)).lookup(anyString());
    }

    @Test
    void runWarmup_skipsASingleMissingWord_andKeepsGoing() {
        // First word is a stopword the upstream does not serve ("of"), the rest are fine.
        // One empty response must NOT stop the run (measured: abort-on-first stopped at word 1).
        when(dictionaryService.lookup(anyString()))
                .thenReturn("[]")
                .thenReturn("[{\"word\":\"a\"}]")
                .thenReturn("[{\"word\":\"b\"}]");

        ReflectionTestUtils.setField(warmup, "limit", 3);
        int warmed = warmup.runWarmup();

        assertThat(warmed).isEqualTo(2);
        verify(dictionaryService, times(3)).lookup(anyString());
    }

    @Test
    void runWarmup_stopsAfterConsecutiveFailures_upstreamLooksDown() {
        // Every word fails -> the run must stop at MAX_CONSECUTIVE_FAILURES, not grind through 500.
        when(dictionaryService.lookup(anyString())).thenReturn("[]");

        int warmed = warmup.runWarmup();

        assertThat(warmed).isEqualTo(0);
        verify(dictionaryService, times(DictionaryWarmupService.MAX_CONSECUTIVE_FAILURES)).lookup(anyString());
    }

    @Test
    void runWarmup_aFailureThenSuccess_resetsTheBreaker() {
        // 4 failures (below the breaker), then a success, then more successes -> keeps going.
        when(dictionaryService.lookup(anyString()))
                .thenReturn("[]").thenReturn("[]").thenReturn("[]").thenReturn("[]")
                .thenReturn("[{\"word\":\"ok\"}]")
                .thenReturn("[{\"word\":\"ok2\"}]");

        ReflectionTestUtils.setField(warmup, "limit", 6);
        int warmed = warmup.runWarmup();

        assertThat(warmed).isEqualTo(2);
        verify(dictionaryService, times(6)).lookup(anyString());
    }

    @Test
    void runWarmup_swallowsException_andCountsItAsAFailure() {
        // A thrown exception counts toward the breaker rather than crashing the run.
        when(dictionaryService.lookup(anyString())).thenThrow(new RuntimeException("upstream exploded"));

        int warmed = warmup.runWarmup();

        assertThat(warmed).isEqualTo(0);
        verify(dictionaryService, times(DictionaryWarmupService.MAX_CONSECUTIVE_FAILURES)).lookup(anyString());
    }

    @Test
    void limitCapsTheNumberOfLookups() throws Exception {
        ReflectionTestUtils.setField(warmup, "limit", 3);
        when(dictionaryService.lookup(anyString())).thenReturn("[{\"word\":\"a\"}]");
        assertThat(warmup.runWarmup()).isEqualTo(3);
        verify(dictionaryService, times(3)).lookup(anyString());
    }
}
