package com.datn.engflow.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubeTranscriptServiceParsingTest {

    private static String json3(String events) {
        return "{\"events\":[" + events + "]}";
    }

    @Test
    void parsesJson3EventsIntoTimedLines() {
        String json = json3(
                "{\"tStartMs\":1000,\"dDurationMs\":2500,\"segs\":[{\"utf8\":\"Hello \"},{\"utf8\":\"world.\"}]}," +
                "{\"tStartMs\":4000,\"dDurationMs\":2000,\"segs\":[{\"utf8\":\"Second line.\"}]}");

        List<com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine> lines =
                YouTubeTranscriptService.parseXmlCaptions(json);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).start()).isEqualTo(1.0);
        assertThat(lines.get(0).end()).isEqualTo(3.5);
        assertThat(lines.get(0).textEn()).isEqualTo("Hello world.");
        assertThat(lines.get(1).start()).isEqualTo(4.0);
        assertThat(lines.get(1).textEn()).isEqualTo("Second line.");
    }

    @Test
    void skipsEmptyEventsAndExtendsPreviousCueWhenDurationMissing() {
        String json = json3(
                "{\"tStartMs\":0,\"dDurationMs\":1000,\"segs\":[{\"utf8\":\"First\"}]}," +
                "{\"tStartMs\":2000,\"segs\":[]}," +
                "{\"tStartMs\":3000,\"segs\":[{\"utf8\":\"Third\"}]}");

        List<com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine> lines =
                YouTubeTranscriptService.parseXmlCaptions(json);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).textEn()).isEqualTo("First");
        assertThat(lines.get(0).end()).isEqualTo(3.0);
        assertThat(lines.get(1).textEn()).isEqualTo("Third");
    }

    @Test
    void extractsVideoIdFromCommonUrlShapes() {
        YouTubeTranscriptService service = new YouTubeTranscriptService(null, null);
        assertThat(service.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")).isEqualTo("dQw4w9WgXcQ");
        assertThat(service.extractVideoId("https://youtu.be/dQw4w9WgXcQ")).isEqualTo("dQw4w9WgXcQ");
        assertThat(service.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ")).isEqualTo("dQw4w9WgXcQ");
        assertThat(service.extractVideoId("dQw4w9WgXcQ")).isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void parsesSrvXmlCaptionsWithEntities() {
        String xml = "<transcript><text start=\"1.24\" dur=\"2.5\">Hello &amp; welcome</text>"
                + "<text start=\"4.0\" dur=\"3.1\">Second &lt;line&gt;</text>"
                + "<text start=\"8.0\" dur=\"1.0\">   </text></transcript>";

        List<com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine> lines =
                YouTubeTranscriptService.parseXmlCaptions(xml);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).start()).isEqualTo(1.24);
        assertThat(lines.get(0).end()).isEqualTo(3.74);
        assertThat(lines.get(0).textEn()).isEqualTo("Hello & welcome");
        assertThat(lines.get(1).textEn()).isEqualTo("Second <line>");
    }

    @Test
    void parsesInnerTubeFormat3WithMillisecondTimings() {
        String xml = "<timedtext format=\"3\"><body>"
                + "<p t=\"1360\" d=\"1680\">[Music]</p>"
                + "<p t=\"18640\" d=\"3240\">We&#39;re no strangers to love</p>"
                + "<p t=\"22640\" d=\"4320\">You know the rules &amp; so do I</p>"
                + "</body></timedtext>";

        List<com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine> lines =
                YouTubeTranscriptService.parseXmlCaptions(xml);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).start()).isCloseTo(18.64, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(lines.get(0).end()).isCloseTo(21.88, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(lines.get(0).textEn()).isEqualTo("We're no strangers to love");
        assertThat(lines.get(1).textEn()).isEqualTo("You know the rules & so do I");
    }

    @Test
    void musicMarkerOnlyLinesAreDropped() {
        assertThat(YouTubeTranscriptService.isMusicMarkerOnly("[Music]")).isTrue();
        assertThat(YouTubeTranscriptService.isMusicMarkerOnly("♪♪♪")).isTrue();
        assertThat(YouTubeTranscriptService.isMusicMarkerOnly("Hello world")).isFalse();
    }

    @Test
    void routesToCorrectParserByContentShape() {
        assertThat(YouTubeTranscriptService.parseXmlCaptions("{\"events\":[]}")).isEmpty();
        assertThat(YouTubeTranscriptService.parseXmlCaptions("<transcript></transcript>")).isEmpty();
    }
}
