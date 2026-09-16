package com.datn.engflow.security;

import com.datn.engflow.exception.BadRequestException;

import java.util.Locale;
import java.util.Set;

/**
 * audit-v8 F81: allowlist for files written to {@code uploads/} and handed back by
 * {@code GET /api/resources/{filename}}. That route is permitAll and same-origin
 * with the SPA, so before this guard any logged-in learner could post a
 * {@code lesson.html} through {@code /api/lesson-submissions/upload-audio} and have
 * the browser render it as first-party HTML -- a stored XSS that reads the JWT out
 * of localStorage (verified end to end in a real browser). The extension is
 * validated when the file is written, and the response type is pinned from the
 * name (never probed from content) when it is read, so a browser-active file that
 * is already on disk cannot be rendered either.
 */
public final class SafeUploadNames {

    /** What the UI actually uploads: accept="image/*", accept="audio/*", recorder webm/ogg. */
    private static final Set<String> BINARY = Set.of(
            "png", "jpg", "jpeg", "gif", "webp",
            "mp3", "wav", "m4a", "aac", "ogg", "oga", "opus", "weba", "webm", "mp4", "mov");

    /** Accepted on upload but served as opaque bytes (transcript imports: .txt/.srt/.vtt). */
    private static final Set<String> OPAQUE = Set.of("txt", "text", "log", "md", "csv", "json", "srt", "vtt");

    private SafeUploadNames() {
    }

    /**
     * Lower-cased extension of an uploaded filename, without the leading dot.
     *
     * @throws BadRequestException when the name has no extension, or an extension a
     *                             browser would execute or render (html, svg, js, ...)
     */
    public static String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            throw new BadRequestException("Thiếu tên tệp tải lên");
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            throw new BadRequestException("Tệp tải lên phải có phần mở rộng");
        }
        String ext = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ext.matches("[a-z0-9]{1,8}")) {
            throw new BadRequestException("Phần mở rộng tệp không hợp lệ");
        }
        if (!BINARY.contains(ext) && !OPAQUE.contains(ext)) {
            throw new BadRequestException("Loại tệp không được hỗ trợ: " + ext);
        }
        return ext;
    }

    /**
     * Content type to serve {@code filename} with, derived from its name only.
     * Unknown or browser-active extensions resolve to opaque bytes with an
     * attachment disposition: this method never throws, so a file that reached the
     * disk before the allowlist existed is neutralised rather than answered with 500.
     */
    public static String contentTypeFor(String filename) {
        String ext = extensionOfOrNull(filename);
        if (ext == null || !BINARY.contains(ext)) {
            return "application/octet-stream";
        }
        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "m4a" -> "audio/mp4";
            case "aac" -> "audio/aac";
            case "ogg", "oga" -> "audio/ogg";
            case "opus" -> "audio/opus";
            case "weba" -> "audio/webm";
            case "webm" -> "video/webm";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            default -> "application/octet-stream";
        };
    }

    /** True when the name must be downloaded rather than displayed. */
    public static boolean forceDownload(String filename) {
        String ext = extensionOfOrNull(filename);
        return ext == null || !BINARY.contains(ext);
    }

    private static String extensionOfOrNull(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ext.matches("[a-z0-9]{1,8}") ? ext : null;
    }
}
