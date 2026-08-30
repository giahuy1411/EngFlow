package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for SRT and WebVTT transcripts into normalized subtitle cues.
 *
 * <p>Supports both {@code 00:01:02,500} (SRT) and {@code 00:01:02.500} (VTT)
 * timestamps, optional {@code HH:MM} short form, and inline VTT cue settings
 * (e.g. {@code 00:00:01.000 --> 00:00:04.000 align:start position:0%}).</p>
 */
@Slf4j
public final class SubtitleParser {

    private static final Pattern TIMESTAMP =
            Pattern.compile("(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})[.,](\\d{1,3})");
    private static final Pattern CUE_LINE =
            Pattern.compile("^\\s*(\\d{1,2}:)?\\d{1,2}:\\d{2}[.,]\\d{1,3}\\s*-->\\s*(\\d{1,2}:)?\\d{1,2}:\\d{2}[.,]\\d{1,3}");

    private SubtitleParser() {
    }

    /**
     * One subtitle cue: start/end seconds plus plain text (HTML tags stripped,
     * multi-line cues joined with a single space).
     */
    public record Cue(double start, double end, String text) {
    }

    public static List<Cue> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Transcript rỗng — hãy dán nội dung SRT/VTT hoặc JSON");
        }
        String normalized = raw.replace("\r\n", "\n").replace("\r", "\n");
        if (normalized.trim().startsWith("[")) {
            throw new BadRequestException("Transcript đã là JSON — gửi trực tiếp cho API, không cần parse");
        }

        List<Cue> cues = new ArrayList<>();
        String[] lines = normalized.split("\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            if (CUE_LINE.matcher(line).find()) {
                double[] times = parseTimes(line);
                List<String> textLines = new ArrayList<>();
                i++;
                while (i < lines.length && !lines[i].isBlank() && !CUE_LINE.matcher(lines[i]).find()) {
                    textLines.add(lines[i]);
                    i++;
                }
                String text = cleanText(String.join(" ", textLines));
                if (!text.isBlank() && times != null) {
                    cues.add(new Cue(times[0], times[1], text));
                }
            } else {
                i++;
            }
        }
        if (cues.isEmpty()) {
            throw new BadRequestException("Không nhận diện được dòng phụ đề nào (định dạng SRT/VTT?)");
        }
        return cues;
    }

    /** Extract [start, end] seconds from a cue timing line. */
    private static double[] parseTimes(String line) {
        Matcher m = TIMESTAMP.matcher(line);
        List<double[]> found = new ArrayList<>();
        while (m.find() && found.size() < 2) {
            found.add(toSeconds(m));
        }
        if (found.size() < 2) {
            return null;
        }
        return new double[]{found.get(0)[0], found.get(1)[0]};
    }

    private static double[] toSeconds(Matcher m) {
        int hours = m.group(1) == null ? 0 : Integer.parseInt(m.group(1));
        int minutes = Integer.parseInt(m.group(2));
        int seconds = Integer.parseInt(m.group(3));
        int fraction = m.group(4) == null ? 0 : Integer.parseInt(padFraction(m.group(4)));
        return new double[]{hours * 3600 + minutes * 60 + seconds + fraction / 1000.0};
    }

    private static String padFraction(String fraction) {
        return switch (fraction.length()) {
            case 1 -> fraction + "00";
            case 2 -> fraction + "0";
            default -> fraction.length() > 3 ? fraction.substring(0, 3) : fraction;
        };
    }

    private static String cleanText(String text) {
        return text
                .replaceAll("<[^>]+>", "")      // <i>, <b>, <c>...
                .replaceAll("\\{[^}]*}", "")    // {\an8} style tags
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
