package com.datn.engflow;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness drift guard — fails the build when tooling or docs reference something
 * the code no longer has.
 *
 * <h2>Why this exists (audit-v15 hardening, weakness L3)</h2>
 *
 * The audit tooling under {@code sweep/**} and the docs are NOT compiled, so nothing
 * notices when a controller endpoint, a DB table, or an enum is removed and the tooling
 * keeps calling it. In audit-v15 the Lesson Builder removal left:
 * <ul>
 *   <li>{@code sweep/v12/api-sweep.js} asserting {@code GET /api/lessons/{id}/structure}
 *       returns 200 — the endpoint was gone, so the sweep reported a false failure;</li>
 *   <li>{@code sweep/v8/p16-parity.sql} counting {@code lesson_snapshots} — a dropped
 *       table, so the documented parity command died with {@code Msg 208};</li>
 *   <li>{@code AGENTS.md} and {@code CLAUDE.md} listing {@code BlockType} / {@code p1–p5}
 *       as if they still existed.</li>
 * </ul>
 * Each was found by hand, late. This test finds them automatically on every
 * {@code mvnw test}, so the next removal cannot silently rot the tooling.
 *
 * <p>It reads the repo as text — no Spring context, no DB — so it is fast and cannot be
 * perturbed by data state. It is deliberately conservative: it only flags a reference
 * when the referenced thing is provably absent from the source tree, and it has an
 * explicit allow-list for intentional "this was removed" assertions.
 */
class HarnessDriftTest {

    /** Repo root: surefire runs with the module dir as CWD. */
    private static final Path ROOT = Paths.get("").toAbsolutePath();

    /**
     * Removed API paths that harnesses assert on PURPOSE (a negative assertion:
     * "this endpoint is gone, expect 404"). Referencing these is correct, not drift.
     */
    private static final Set<String> INTENTIONAL_REMOVED_PATHS = Set.of(
            "/api/lessons/{id}/structure",
            "/api/admin/lessons/{id}/structure",
            "/api/admin/lessons/{id}/snapshots",
            "/api/admin/sections/{id}",
            "/api/admin/blocks/{id}"
    );

    /** Symbols removed in audit-v15 that docs must not list as existing. */
    private static final List<String> REMOVED_SYMBOLS = List.of(
            "LessonStructureController", "LessonStructureService",
            "LessonSnapshotController", "LessonSnapshotService",
            "LessonBlock", "LessonSection", "LessonSnapshot",
            "BlockType", "QuestionType",
            "AdminLessonBuilder", "LessonBlocks"
    );

    /** Files whose job is to RECORD a removal — they may mention removed names. */
    private static boolean isRemovalRecord(String rel) {
        String r = rel.replace('\\', '/').toLowerCase(Locale.ROOT);
        return r.contains("lesson-builder-removal")
                || r.contains("harness-restore")
                || r.contains("cleanup-manifest")
                || r.contains("deleted-files")
                || r.contains("/.specify/specs/")
                || r.contains("harnessdrift")
                // One-off repair scripts from earlier audits: frozen historical records,
                // not live tooling. They reference endpoints as they were at the time.
                || r.startsWith("scripts/")
                // Third-party code — never ours to fix.
                || r.contains("node_modules/");
    }

    /** Paths under a directory, skipping anything we must never scan. */
    private static List<Path> scanFiles(Path dir, String suffix) throws IOException {
        return filesUnder(dir, suffix).stream()
                .filter(p -> !rel(p).contains("node_modules/"))
                .collect(Collectors.toList());
    }

    private static List<Path> filesUnder(Path dir, String suffix) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(suffix))
                    .collect(Collectors.toList());
        }
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private static String rel(Path p) {
        return ROOT.relativize(p).toString().replace('\\', '/');
    }

    // ---------------------------------------------------------------- endpoint paths

    /**
     * Collect the API paths the controllers actually expose.
     *
     * <p>Deliberately mirrors {@code sweep/harness/api-inventory.js}: read the class-level
     * {@code @RequestMapping} and each verb mapping, then join. Good enough to catch a
     * reference to a controller that no longer exists — which is the failure mode here.
     */
    private Set<String> exposedPaths() throws IOException {
        Set<String> out = new TreeSet<>();
        Path ctrl = ROOT.resolve("src/main/java/com/datn/engflow/controller");
        // Class-level base path (single string).
        Pattern classMapping = Pattern.compile("@RequestMapping\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"");
        // Verb mapping: the argument list may be absent, a single string, or an array
        // of strings ({"a","b"}). Take the whole balanced (...) then harvest every
        // quoted literal inside, skipping named args that are not paths.
        Pattern verbStart = Pattern.compile("@(?:Get|Post|Put|Patch|Delete)Mapping");
        for (Path f : filesUnder(ctrl, ".java")) {
            String src = read(f);
            String base = "";
            Matcher cm = classMapping.matcher(src);
            if (cm.find()) base = cm.group(1);
            Matcher vs = verbStart.matcher(src);
            while (vs.find()) {
                String args = balancedArgs(src, vs.end());
                if (args == null) {
                    // Bare `@GetMapping` with no argument list: it serves the class-level
                    // base path itself (e.g. AdminExerciseController's @RequestMapping).
                    if (!base.isEmpty()) out.add(base.replaceAll("/{2,}", "/"));
                    continue;
                }
                // Drop non-path named args so their literals are not harvested.
                String cleaned = args.replaceAll(
                        "\\b(params|headers|produces|consumes|name)\\s*=\\s*(\\{[^}]*}|\"[^\"]*\")", "");
                Matcher lit = Pattern.compile("\"([^\"]*)\"").matcher(cleaned);
                while (lit.find()) {
                    String sub = lit.group(1);
                    String full = (base + sub).replaceAll("/{2,}", "/");
                    if (full.startsWith("/")) out.add(full);
                }
            }
        }
        return out;
    }

    /**
     * Return the text inside the balanced {@code ( ... )} that follows {@code from},
     * or null when the next non-space char is not {@code (}. Balanced rather than a
     * regex so a nested {@code {..}} array or a string containing ")" cannot truncate it.
     */
    private static String balancedArgs(String src, int from) {
        int i = from;
        while (i < src.length() && Character.isWhitespace(src.charAt(i))) i++;
        if (i >= src.length() || src.charAt(i) != '(') return null;
        int depth = 0;
        int start = i + 1;
        for (; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return src.substring(start, i);
            }
        }
        return src.substring(start);
    }

    /**
     * Split a path into segments, marking WILDCARDS. A segment is a wildcard when it is a
     * declared path variable ({@code {id}}, {@code {lessonId:.*}}) or a concrete id the
     * harness substituted in ({@code 41881}). Literal segments stay literal, so
     * {@code /api/lessons/41881/exercises} and {@code /api/lessons/{lessonId}/exercises}
     * both become ["api","lessons","*","exercises"].
     */
    private static List<String> segments(String p) {
        String[] parts = p.split("/");
        List<String> out = new ArrayList<>();
        for (String seg : parts) {
            if (seg.isEmpty()) continue;
            String s = seg;
            int colon = s.indexOf(':');
            if (colon > 0 && s.startsWith("{")) s = s.substring(0, colon);   // {id:.*} -> {id}
            boolean wild = (s.startsWith("{") && s.endsWith("}")) || s.matches("\\d+");
            out.add(wild ? "*" : s);
        }
        return out;
    }

    /** True when `ref` is a leading portion of `exposed` (segment-wise, "*" matches any). */
    private static boolean isPrefix(List<String> ref, List<String> exposed) {
        if (ref.size() > exposed.size()) return false;
        for (int i = 0; i < ref.size(); i++) {
            if (ref.get(i).equals("*") || exposed.get(i).equals("*")) continue;
            if (!ref.get(i).equals(exposed.get(i))) return false;
        }
        return true;
    }

    /** True when the reference matches an exposed path, treating "*" as any single segment. */
    private static boolean matches(List<String> ref, List<String> exposed) {
        if (ref.size() != exposed.size()) return false;
        for (int i = 0; i < ref.size(); i++) {
            if (ref.get(i).equals("*") || exposed.get(i).equals("*")) continue;
            if (!ref.get(i).equals(exposed.get(i))) return false;
        }
        return true;
    }

    @Test
    void harnessDoesNotCallEndpointsThatNoLongerExist() throws IOException {
        Set<String> exposed = exposedPaths();
        assertThat(exposed).as("controller scan must find endpoints").isNotEmpty();
        List<List<String>> exposedSegs = exposed.stream()
                .map(HarnessDriftTest::segments).collect(Collectors.toList());

        // Scan the tracked tooling + docs for `/api/...` literals.
        List<Path> targets = new ArrayList<>();
        targets.addAll(scanFiles(ROOT.resolve("sweep"), ".js"));
        targets.addAll(scanFiles(ROOT.resolve("sweep"), ".sql"));
        targets.add(ROOT.resolve("AGENTS.md"));

        Pattern apiRef = Pattern.compile("(/api/[A-Za-z0-9_{}$:./\\-]*)");
        List<String> problems = new ArrayList<>();
        for (Path f : targets) {
            if (!Files.isRegularFile(f)) continue;
            String r = rel(f);
            if (isRemovalRecord(r)) continue;
            int line = 0;
            for (String text : read(f).split("\n")) {
                line++;
                // A line that narrates a PAST defect ("used to point at X", "after the fix")
                // quotes the bad path on purpose — a historical note, not a claim that the
                // path exists. Same idea as the docs check below.
                String low = text.toLowerCase(Locale.ROOT);
                if (low.contains("từng") || low.contains("was wrong") || low.contains("used to")
                        || low.contains("sau fix") || low.contains("after the fix")
                        || low.contains("404") || low.contains("no longer") || low.contains("đã gỡ")) {
                    continue;
                }
                Matcher m = apiRef.matcher(text);
                while (m.find()) {
                    String ref = m.group(1).replaceAll("[/.,;'\")]+$", "");
                    // Only judge things that look like a real API reference: at least
                    // /api/<segment>/<segment>. "/api/admin" alone is a prefix used in
                    // role tests and prose, not an endpoint claim.
                    if (ref.split("/").length < 4) continue;
                    if (INTENTIONAL_REMOVED_PATHS.stream().anyMatch(ref::contains)) continue;
                    // Template literals (${...}) can't be resolved statically — skip them
                    // rather than report a false positive.
                    if (ref.contains("$")) continue;
                    List<String> refSegs = segments(ref);
                    if (refSegs.size() < 2) continue;
                    // Match, OR the reference is a PREFIX of a real endpoint. A prefix is
                    // how docs/tooling legitimately name a whole controller
                    // ("/api/admin/exercises") or a family. A prefix can never hide a
                    // removed endpoint, which is what this guard is for.
                    boolean ok = exposedSegs.stream().anyMatch(e -> matches(refSegs, e))
                            || exposedSegs.stream().anyMatch(e -> isPrefix(refSegs, e));
                    if (!ok) problems.add(r + ":" + line + " -> " + ref);
                }
            }
        }
        assertThat(problems)
                .as("Tooling/docs reference API paths that no controller exposes. "
                        + "Either fix the reference or add it to INTENTIONAL_REMOVED_PATHS if it is "
                        + "a deliberate 'endpoint was removed' assertion.")
                .isEmpty();
    }

    // ---------------------------------------------------------------- DB tables

    /**
     * Table names the harness writes to / counts must still be declared by an entity
     * (schema is entity-driven: {@code ddl-auto=update}, Flyway disabled).
     */
    @Test
    void harnessDoesNotReferenceDroppedTables() throws IOException {
        Set<String> entityTables = new TreeSet<>();
        Path entityDir = ROOT.resolve("src/main/java/com/datn/engflow/model/entity");
        Pattern table = Pattern.compile("@Table\\s*\\(\\s*name\\s*=\\s*\"([^\"]+)\"");
        for (Path f : filesUnder(entityDir, ".java")) {
            Matcher m = table.matcher(read(f));
            while (m.find()) entityTables.add(m.group(1));
        }
        assertThat(entityTables).as("entity scan must find @Table names").isNotEmpty();

        // Tables dropped by audit-v15 may be NAMED in comments / removal records, but must
        // never appear in executable SQL — a live FROM on a dropped table is exactly the
        // drift this test exists to catch (p16-parity.sql died with Msg 208 on lesson_snapshots).
        // Comments are stripped below, so a documented mention does not trip it.
        List<String> problems = new ArrayList<>();
        for (Path f : filesUnder(ROOT.resolve("sweep"), ".sql")) {
            String r = rel(f);
            if (isRemovalRecord(r)) continue;
            // Strip `--` line comments first: a comment naming a dropped table (or saying
            // "reachable from swept endpoints") is prose, not SQL.
            String src = read(f).replaceAll("(?m)--.*$", "");
            Matcher m = Pattern.compile("(?i)\\bFROM\\s+([a-z_][a-z0-9_]*)").matcher(src);
            while (m.find()) {
                String t = m.group(1).toLowerCase(Locale.ROOT);
                if (t.equals("sys") || t.equals("information_schema") || t.equals("tempdb")) continue;
                if (!entityTables.contains(t)) problems.add(r + " -> FROM " + t);
            }
        }
        assertThat(problems)
                .as("Harness SQL reads tables no entity declares (table was renamed or dropped).")
                .isEmpty();
    }

    // ---------------------------------------------------------------- docs

    @Test
    void docsDoNotListRemovedSymbolsAsExisting() throws IOException {
        // Only these docs describe the CURRENT system; audit evidence is a historical
        // record and may legitimately name what it removed.
        List<Path> docs = List.of(
                ROOT.resolve("AGENTS.md"),
                ROOT.resolve("CLAUDE.md"),
                ROOT.resolve("README.md"),
                ROOT.resolve("docs/erd-sql-guide.md")
        );
        List<String> problems = new ArrayList<>();
        for (Path f : docs) {
            if (!Files.isRegularFile(f)) continue;
            String r = rel(f);
            if (isRemovalRecord(r)) continue;
            int line = 0;
            for (String text : read(f).split("\n")) {
                line++;
                String lower = text.toLowerCase(Locale.ROOT);
                // A line that EXPLAINS a removal is fine ("... was removed", "đã gỡ", "dropped").
                boolean explains = lower.contains("removed") || lower.contains("dropped")
                        || lower.contains("đã gỡ") || lower.contains("đã xoá") || lower.contains("đã drop")
                        || lower.contains("no longer") || lower.contains("không còn");
                if (explains) continue;
                for (String sym : REMOVED_SYMBOLS) {
                    if (text.contains(sym)) problems.add(r + ":" + line + " mentions " + sym + " -> " + text.trim());
                }
            }
        }
        assertThat(problems)
                .as("Docs describe removed symbols as if they still exist. Reword to say they were "
                        + "removed, or drop the reference.")
                .isEmpty();
    }
}
