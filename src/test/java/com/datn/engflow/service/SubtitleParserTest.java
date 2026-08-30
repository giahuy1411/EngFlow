package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.service.SubtitleParser.Cue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubtitleParserTest {

    @Test
    void parsesSrtWithCommaTimestamps() {
        String srt = """
                1
                00:00:01,000 --> 00:00:04,500
                Hello, dear English learners.

                2
                00:00:05,000 --> 00:00:08,250
                Welcome to today's episode.
                """;
        List<Cue> cues = SubtitleParser.parse(srt);
        assertThat(cues).hasSize(2);
        assertThat(cues.get(0).start()).isEqualTo(1.0);
        assertThat(cues.get(0).end()).isEqualTo(4.5);
        assertThat(cues.get(0).text()).isEqualTo("Hello, dear English learners.");
        assertThat(cues.get(1).start()).isEqualTo(5.0);
        assertThat(cues.get(1).end()).isEqualTo(8.25);
    }

    @Test
    void parsesVttWithHeaderAndDotTimestamps() {
        String vtt = """
                WEBVTT

                00:00:01.000 --> 00:00:04.000 align:start position:0%
                Listen, <i>learn</i>, and improve.

                00:00:04.500 --> 00:00:07.000
                Your English effortlessly.
                """;
        List<Cue> cues = SubtitleParser.parse(vtt);
        assertThat(cues).hasSize(2);
        assertThat(cues.get(0).text()).isEqualTo("Listen, learn, and improve.");
    }

    @Test
    void joinsMultiLineCuesAndStripsTags() {
        String srt = "1\n"
                + "00:01:02,000 --> 00:01:05,000\n"
                + "Line one\n"
                + "line {\\an8}two\n";
        List<Cue> cues = SubtitleParser.parse(srt);
        assertThat(cues.get(0).text()).isEqualTo("Line one line two");
        assertThat(cues.get(0).start()).isEqualTo(62.0);
    }

    @Test
    void rejectsEmptyAndUnrecognizedInput() {
        assertThatThrownBy(() -> SubtitleParser.parse("   "))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> SubtitleParser.parse("just some plain text"))
                .isInstanceOf(BadRequestException.class);
    }
}
