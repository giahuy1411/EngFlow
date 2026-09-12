package com.datn.engflow.service;

import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.repository.ExerciseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.annotation.PreDestroy;

@Service
@Slf4j
public class AiAnswerBackfillService {

    private final ObjectMapper objectMapper;
    private final ExerciseRepository exerciseRepository;

    @Value("${ai.exercise.ollama.base-url:http://localhost:11434/v1}")
    private String ollamaBaseUrl;

    @Value("${ai.exercise.ollama.model:qwen2.5:1.5b}")
    private String model;

    private final ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "answer-backfill");
        t.setDaemon(true);
        return t;
    });

    private final ConcurrentHashMap<String, BackfillProgress> progressMap = new ConcurrentHashMap<>();
    private final AtomicBoolean runningFlag = new AtomicBoolean(false);
    private volatile long checkpointLessonId = 0L;

    public AiAnswerBackfillService(ObjectMapper objectMapper, ExerciseRepository exerciseRepository) {
        this.objectMapper = objectMapper;
        this.exerciseRepository = exerciseRepository;
    }

    // ── Progress DTO ──

    public static class BackfillProgress {
        public int totalExercises;
        public int processed;
        public int backfilled;
        public int deterministic;
        public int aiFilled;
        public int unfillable;
        public int errors;
        public boolean running;
        public boolean dryRun;
        public String currentLesson = "";
        public long checkpointLessonId;
        public long elapsedMs;
        public List<String> errorDetails = new ArrayList<>();
        public List<String> unfillableSamples = new ArrayList<>();
    }

    // ── Public API ──

    public boolean isRunning() { return runningFlag.get(); }
    public long getCheckpointLessonId() { return checkpointLessonId; }
    public BackfillProgress getProgress(String batchId) { return progressMap.get(batchId); }

    public synchronized String startBackfill(boolean dryRun, int limit, boolean restart) {
        return startBackfill(dryRun, limit, restart, null);
    }

    /**
     * Starts a backfill batch.
     *
     * @param dryRun          measure only, persist nothing
     * @param limit           whole-lesson budget (0 = all); oversized lessons are
     *                        deferred whole, never split (see bug (c) note in run)
     * @param restart         ignore the lesson checkpoint and start from 0
     * @param lessonIdInScope when non-null, restrict the batch to that single lesson
     *                        and do not advance the durable checkpoint (a scoped
     *                        run must not mark unseen lessons done)
     * @return the batch id to poll
     */
    public synchronized String startBackfill(boolean dryRun, int limit, boolean restart, Long lessonIdInScope) {
        for (Map.Entry<String, BackfillProgress> e : progressMap.entrySet()) {
            if (e.getValue().running) return e.getKey();
        }
        String batchId = UUID.randomUUID().toString();
        BackfillProgress p = new BackfillProgress();
        p.running = true;
        p.dryRun = dryRun;
        progressMap.put(batchId, p);
        runningFlag.set(true);
        long fromLesson = (restart ? 0L : checkpointLessonId);
        CompletableFuture.runAsync(() -> run(dryRun, limit, fromLesson, lessonIdInScope, p, batchId), pool);
        return batchId;
    }

    private void run(boolean dryRun, int limit, long fromLessonExclusive, Long lessonIdInScope,
                     BackfillProgress progress, String batchId) {
        long t0 = System.currentTimeMillis();
        try {
            List<Exercise> candidates = exerciseRepository.findBackfillCandidates(Pageable.unpaged());
            if (lessonIdInScope != null) {
                candidates.removeIf(e -> e.getLesson() == null || e.getLesson().getId() == null
                        || !lessonIdInScope.equals(e.getLesson().getId()));
            } else if (fromLessonExclusive > 0) {
                candidates.removeIf(e -> e.getLesson() != null && e.getLesson().getId() != null
                        && e.getLesson().getId() <= fromLessonExclusive);
            }

            // LinkedHashMap keyed by lesson id, then re-sorted ascending: the
            // durable checkpoint is a max-lesson-id, so whole-lesson truncation
            // must drop only lessons with ids ABOVE every processed one —
            // iterating candidates in exercise-id order could otherwise leave a
            // lower-id lesson behind the checkpoint (same stranding as bug (c)).
            Map<Long, List<Exercise>> grouped = new LinkedHashMap<>();
            for (Exercise e : candidates) {
                Long lid = e.getLesson() != null ? e.getLesson().getId() : 0L;
                grouped.computeIfAbsent(lid, k -> new ArrayList<>()).add(e);
            }
            Map<Long, List<Exercise>> byLesson = new java.util.TreeMap<>(grouped);

            // limit is applied at whole-lesson boundaries (bug (c)): a lesson whose
            // rows exceed the remaining budget is dropped in full — the old
            // subList(0, limit) could fill part of a lesson while the checkpoint
            // advanced past its id, stranding the tail forever on resume.  Every
            // lesson after the first oversized one is dropped too, so the
            // ID-max checkpoint can never skip over a hole.
            if (limit > 0) {
                int budget = limit;
                boolean truncated = false;
                Iterator<Map.Entry<Long, List<Exercise>>> it = byLesson.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Long, List<Exercise>> entry = it.next();
                    if (truncated || entry.getValue().size() > budget) {
                        it.remove();
                        truncated = true;
                        continue;
                    }
                    budget -= entry.getValue().size();
                }
            }
            for (List<Exercise> rows : byLesson.values()) progress.totalExercises += rows.size();

            for (Map.Entry<Long, List<Exercise>> entry : byLesson.entrySet()) {
                List<Exercise> rows = entry.getValue();
                Lesson lesson = rows.get(0).getLesson();
                progress.currentLesson = lesson != null ? lesson.getTitle() : ("lesson " + entry.getKey());
                try {
                    processLesson(lesson, rows, progress, dryRun);
                } catch (Exception ex) {
                    progress.errors++;
                    if (progress.errorDetails.size() < 50) progress.errorDetails.add("lesson " + entry.getKey() + ": " + ex.getMessage());
                    log.error("Backfill lesson {} failed: {}", entry.getKey(), ex.getMessage());
                }
                progress.processed = Math.min(progress.processed + rows.size(), progress.totalExercises);
                long lid = entry.getKey() == null ? 0L : entry.getKey();
                // Bug (b): a dry-run must not move the durable checkpoint — the
                // rows were never written, so skipping them on the next real
                // run would strand them exactly like bug (c).  Scoped lesson runs
                // likewise never advance it (they leave lessons unseen).
                if (!dryRun && lessonIdInScope == null)
                    checkpointLessonId = Math.max(checkpointLessonId, lid);
                progress.checkpointLessonId = checkpointLessonId;
                progress.elapsedMs = System.currentTimeMillis() - t0;
            }
        } catch (Exception ex) {
            progress.errors++;
            if (progress.errorDetails.size() < 50) progress.errorDetails.add("run: " + ex.getMessage());
            log.error("Backfill run failed: {}", ex.getMessage(), ex);
        } finally {
            progress.running = false;
            progress.elapsedMs = System.currentTimeMillis() - t0;
            runningFlag.set(false);
            log.info("Backfill {} done: total={} backfilled={} (det={} ai={}) unfillable={} errors={} in {}ms{}",
                    batchId, progress.totalExercises, progress.backfilled, progress.deterministic,
                    progress.aiFilled, progress.unfillable, progress.errors, progress.elapsedMs,
                    progress.dryRun ? " [DRY-RUN]" : "");
        }
    }

    // ── Per-lesson pipeline ──

    private void processLesson(Lesson lesson, List<Exercise> exercises, BackfillProgress progress, boolean dryRun) {
        String content = lesson != null ? lesson.getContent() : null;
        // The seed scraper put <details><summary>ANSWER</summary>...</details>
        // blocks that contain plain-text answer keys.  Seed lessons stack
        // several blocks whose numbering restarts at 1, so parse each block
        // into its own map and align blocks to exercises in document order
        // instead of flattening everything into one sequence.
        List<Map<Integer, String>> answerBlocks = (content == null || content.isBlank())
                ? List.of()
                : parseAnswerBlocks(content);

        boolean dirty = false;
        int filled = 0;
        int blockIdx = 0;   // index into answerBlocks, advanced as slots are consumed
        int posInBlock = 0;  // next slot position inside the current block

        for (Exercise ex : exercises) {
            String answer = null;
            String source = null;

            // MC fragments ("a He said…", "……", "hotel", etc.) are not
            // self-contained prompts for the text-input grader.  Answer-key
            // fragments ("studied 2 moved 3 looked 4 stopped 5 talked") are
            // scraped key rows, never real prompts.  Both are unfillable.
            if (ex.getExerciseType() == ExerciseType.MULTIPLE_CHOICE
                    && (isFragmentQuestion(ex.getQuestion()) || isAnswerKeyFragment(ex.getQuestion()))) {
                progress.unfillable++;
                if (progress.unfillableSamples.size() < 20)
                    progress.unfillableSamples.add(ex.getId() + " [MC-fragment] " + truncate(ex.getQuestion(), 80));
                continue;
            }

            int gaps = ex.getExerciseType() == ExerciseType.FILL_BLANK
                    ? countGaps(ex.getQuestion())
                    : 1;

            // Layer 1a: embedded gap numbers ("Crist11……", "he12……") map
            // straight into the key block that covers them — the most precise
            // alignment signal for multi-gap reading exercises.  The current
            // block is preferred so numbers that exist in several blocks
            // (1..10 in every block) resolve against the block being consumed.
            List<Integer> embeddedNums = embeddedGapNumbers(ex.getQuestion());
            if (!embeddedNums.isEmpty()) {
                String joined = answersForNumbers(answerBlocks, blockIdx, embeddedNums);
                if (joined != null && isValidAnswer(joined, ex)) {
                    answer = joined;
                    source = "answer-key-gapnums";
                    // Advance the cursor past the highest consumed number so
                    // the next exercise stays aligned (block switch included).
                    int maxNum = 0;
                    for (int n : embeddedNums) if (n > maxNum) maxNum = n;
                    if (maxNum > posInBlock) posInBlock = maxNum;
                }
            }

            // Layer 1b: positional slots from the current block, consuming N
            // slots for a multi-gap FILL_BLANK (comma-joined) and 1 otherwise.
            if (answer == null && !answerBlocks.isEmpty()) {
                List<String> slots = takeSlots(answerBlocks, blockIdx, posInBlock, gaps, ex);
                if (slots != null) {
                    String joined = String.join(", ", slots);
                    if (isValidAnswer(joined, ex)) {
                        answer = joined;
                        source = "answer-key-pos";
                    }
                    // Advance the positional cursor by the number of slots
                    // consumed, valid or not, so following exercises stay aligned.
                    posInBlock += Math.max(1, slots.size());
                }
            }

            // Layer 1c: leading exercise number fallback (single-numbered rows).
            // Use only when the positional slot was absent or invalid.
            if (answer == null) {
                Integer num = leadingExerciseNumber(ex.getQuestion());
                if (num != null) {
                    String cand = lookupNumbered(answerBlocks, num);
                    if (cand != null && isValidAnswer(cand, ex)) {
                        answer = cand;
                        source = "answer-key-num";
                    }
                }
            }

            // Layer 2: Ollama for the residue.
            if (answer == null && content != null && !content.isBlank()) {
                String ai = askAi(lesson, ex);
                if (ai != null && isValidAnswer(ai, ex)) {
                    answer = normalize(ai);
                    source = "ai";
                }
            }

            // Advance to the next answer block once the current one is exhausted.
            while (posInBlock > 0 && blockIdx < answerBlocks.size()
                    && posInBlock >= blockLength(answerBlocks.get(blockIdx))) {
                blockIdx++;
                posInBlock = 0;
            }

            if (answer != null) {
                ex.setCorrectAnswer(answer);
                dirty = true;
                filled++;
                progress.backfilled++;
                if ("ai".equals(source)) progress.aiFilled++; else progress.deterministic++;
            } else {
                progress.unfillable++;
                if (progress.unfillableSamples.size() < 20)
                    progress.unfillableSamples.add(ex.getId() + " [" + ex.getExerciseType() + "] " + truncate(ex.getQuestion(), 80));
            }
        }

        if (dirty && !dryRun) exerciseRepository.saveAll(exercises);
        if (filled > 0)
            log.info("Backfill lesson {}: {}/{} filled (dryRun={})",
                    lesson != null ? lesson.getId() : null, filled, exercises.size(), dryRun);
    }

    // ── Layer 1: answer-key extraction ──

    static final Pattern DETAILS_BLOCK = Pattern.compile(
            "<details[^>]*>\\s*<summary[^>]*>\\s*ANSWER\\s*</summary>(.*?)</details>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** Matches "N answer" pairs inside flattened ANSWER text.  Handles both
     *  "1 e 2 d" (letter keys) and "1 taller 2 larger" (word keys). */
    static final Pattern ANSWER_PAIR = Pattern.compile(
            "(?<![0-9])([0-9]{1,3})(?![0-9])\\s*[.)]?\\s*(.*?)(?=(?<![0-9])[0-9]{1,3}(?![0-9])|$)");

    /**
     * Parses every ANSWER block into its own numbered map.  Seed lessons stack
     * several blocks whose numbering restarts at 1 each time; flattening them
     * into one sequence misaligns the positional mapping, so callers work
     * block-by-block instead.
     *
     * @param html lesson HTML containing ANSWER details blocks
     * @return ordered list of number→answer maps, one per ANSWER block
     */
    static List<Map<Integer, String>> parseAnswerBlocks(String html) {
        if (html == null || html.isBlank()) return List.of();
        Matcher details = DETAILS_BLOCK.matcher(html);
        List<Map<Integer, String>> blocks = new ArrayList<>();
        while (details.find()) {
            String text = flattenHtml(details.group(1));
            Map<Integer, String> map = new LinkedHashMap<>();
            Matcher m = ANSWER_PAIR.matcher(text);
            while (m.find()) {
                String ans = m.group(2);
                if (ans == null) continue;
                ans = ans.trim();
                if (ans.isEmpty()) continue;
                int num;
                try {
                    num = Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {
                    continue;
                }
                map.putIfAbsent(num, ans);
            }
            if (!map.isEmpty()) blocks.add(map);
        }
        return blocks;
    }

    /** Returns answers in document order across every ANSWER block (legacy view). */
    static List<String> parseAnswerList(String html) {
        List<String> seq = new ArrayList<>();
        for (Map<Integer, String> block : parseAnswerBlocks(html)) {
            seq.addAll(block.values());
        }
        return seq;
    }

    /** Number→answer map built from the flattened sequence (legacy entry point). */
    static Map<Integer, String> parseAnswerKeyMap(String html) {
        Map<Integer, String> map = new LinkedHashMap<>();
        List<String> seq = parseAnswerList(html);
        for (int i = 0; i < seq.size(); i++) map.putIfAbsent(i + 1, seq.get(i));
        return map;
    }

    // ── Answer-block alignment helpers ──

    /**
     * Detects scraped MC rows that are really fragments of the answer key,
     * e.g. "studied 2 moved 3 looked 4 stopped 5 talked" — two or more
     * numbered pairs whose text starts with a letter means the row is a key,
     * not a prompt.  Dates ("December 2, 1992") do not count: the text after
     * the number starts with punctuation.
     *
     * @param question the MC row text
     * @return true when the row looks like an answer-key fragment
     */
    static boolean isAnswerKeyFragment(String question) {
        if (question == null) return false;
        Matcher m = ANSWER_PAIR.matcher(question.trim());
        int pairs = 0;
        while (m.find()) {
            String tail = m.group(2);
            if (tail != null && tail.matches("[a-zA-Z].*")) pairs++;
        }
        return pairs >= 2;
    }

    /**
     * Extracts gap numbers glued before gaps ("Crist11……" → 11, "he12…" → 12)
     * in question order.
     *
     * @param question the exercise text
     * @return gap numbers in order; empty when none are glued
     */
    static List<Integer> embeddedGapNumbers(String question) {
        if (question == null) return List.of();
        Matcher m = NUM_GLUED_BEFORE_GAP.matcher(question);
        List<Integer> nums = new ArrayList<>();
        while (m.find()) {
            try {
                int v = Integer.parseInt(m.group(1));
                if (v >= 1 && v <= 200) nums.add(v);
            } catch (NumberFormatException ignored) {
                // skip malformed glued number
            }
        }
        return nums;
    }

    /**
     * Looks each embedded gap number up across the blocks and comma-joins the
     * matches.  The block at {@code preferredIdx} is checked first so numbers
     * that appear in several blocks resolve against the block being consumed.
     *
     * @param blocks       parsed answer blocks
     * @param preferredIdx block index to try before the rest (document order)
     * @param nums         gap numbers in question order
     * @return comma-joined answers, or null when any number is missing
     */
    static String answersForNumbers(List<Map<Integer, String>> blocks, int preferredIdx, List<Integer> nums) {
        if (blocks == null || blocks.isEmpty() || nums == null || nums.isEmpty()) return null;
        List<String> parts = new ArrayList<>();
        for (int num : nums) {
            String found = null;
            if (preferredIdx >= 0 && preferredIdx < blocks.size()) {
                found = blocks.get(preferredIdx).get(num);
            }
            if (found == null) {
                for (Map<Integer, String> block : blocks) {
                    String cand = block.get(num);
                    if (cand != null) {
                        found = cand;
                        break;
                    }
                }
            }
            if (found == null) return null;
            parts.add(found);
        }
        return String.join(", ", parts);
    }

    /**
     * Takes up to {@code gaps} consecutive slots starting at {@code startPos}
     * (0-based within the block's ordered values).
     *
     * @param blocks   parsed answer blocks
     * @param blockIdx index of the block being consumed
     * @param startPos 0-based slot position within that block
     * @param gaps     how many slots this exercise needs
     * @param ex       exercise being filled (unused now, kept for signature stability)
     * @return the slot values, or null when the block cannot supply them
     */
    static List<String> takeSlots(List<Map<Integer, String>> blocks, int blockIdx, int startPos, int gaps, Exercise ex) {
        if (blocks == null || blocks.isEmpty() || blockIdx >= blocks.size()) return null;
        List<String> blockValues = new ArrayList<>(blocks.get(blockIdx).values());
        if (startPos < 0 || startPos >= blockValues.size()) return null;
        int need = Math.max(1, gaps);
        int to = Math.min(startPos + need, blockValues.size());
        List<String> slots = new ArrayList<>();
        for (int i = startPos; i < to; i++) slots.add(blockValues.get(i));
        return slots;
    }

    /** Number of entries in a block (cursor-advance threshold). */
    private static int blockLength(Map<Integer, String> block) {
        return block == null ? 1 : Math.max(1, block.size());
    }

    /**
     * Finds an exercise number across all blocks; the first block containing
     * the number wins (document order).
     *
     * @param blocks parsed answer blocks
     * @param num    exercise number
     * @return the answer text, or null when not found
     */
    static String lookupNumbered(List<Map<Integer, String>> blocks, Integer num) {
        if (blocks == null || num == null) return null;
        for (Map<Integer, String> block : blocks) {
            String cand = block.get(num);
            if (cand != null) return cand;
        }
        return null;
    }

    /** Strips HTML tags and decodes common entities. */
    static String flattenHtml(String html) {
        String t = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        t = t.replaceAll("<[^>]+>", " ");
        t = t.replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'");
        return t.replaceAll("\\s+", " ").trim();
    }

    // ── Exercise number extraction ──

    /** Number at start of exercise line, optionally followed by a gap. */
    static final Pattern LEADING_NUM = Pattern.compile("^\\s*([0-9]{1,3})\\s*[.):\\]]?\\s*");

    /** Number glued before a gap: "Crist11……" or "11……" — a letter then digits then a gap. */
    static final Pattern NUM_GLUED_BEFORE_GAP = Pattern.compile("[a-zA-Z]([0-9]{1,3})(?=[.…_])");

    /** Number anywhere in text, bounded by non-digits. */
    static final Pattern NUM_ANY = Pattern.compile("(?<![0-9])\\b([0-9]{1,3})\\b(?![0-9])");

    static Integer leadingExerciseNumber(String question) {
        if (question == null) return null;
        Matcher m = LEADING_NUM.matcher(question);
        if (m.find()) return Integer.parseInt(m.group(1));
        Matcher g = NUM_GLUED_BEFORE_GAP.matcher(question);
        if (g.find()) {
            try { return Integer.parseInt(g.group(1)); } catch (NumberFormatException ignored) {}
        }
        Matcher n = NUM_ANY.matcher(question);
        if (n.find()) {
            try { int v = Integer.parseInt(n.group(1)); return (v >= 1 && v <= 200) ? v : null; } catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    // ── Fragment classification ──

    static final Pattern GAP = Pattern.compile("[.…_]{2,}");

    /** Returns true when the MC row is not a self-contained prompt for a text-input grader. */
    static boolean isFragmentQuestion(String question) {
        if (question == null || question.isBlank()) return true;
        String q = question.trim();
        if (!q.matches(".*[a-zA-Z0-9].*")) return true; // pure placeholder
        if (q.endsWith("?")) return false;                        // real question
        if (GAP.matcher(q).find()) return false;                  // has gap
        if (q.matches(".*\\([a-zA-Z][a-zA-Z ,]+\\).*")) return false; // (verb) hint
        if (q.matches("(?i)^[a-e]\\s.+")) return true;       // scraped option line
        if (q.length() <= 8) return true;                        // bare word fragment
        return true; // statements are not prompts for the text-input grader
    }

    // ── Validation (LLM05: untrusted AI output ──

    static boolean isValidAnswer(String answer, Exercise ex) {
        if (answer == null) return false;
        String a = answer.trim();
        if (a.isEmpty()) return false;
        if (a.length() > 120) return false;
        String lo = a.toLowerCase(Locale.ROOT);
        if (lo.equals("left|right") || lo.contains("left|right")) return false;
        if (lo.matches(".*[.…_]{4,}.*")) return false;
        if (lo.equals("answer") || lo.equals("answers")) return false;
        if (lo.contains("your own answers")) return false;
        if (!lo.matches(".*[a-z].*")) return false;               // no letters at all
        if (lo.matches("^[.…_\\s]+$")) return false;               // answer itself is only placeholder chars
        if (a.matches("(?i)^[a-e]$")) return false;               // bare option letter
        if (OPTION_PREFIX.matcher(a).find()) return false;        // "a He doesn't want…" — scraped option line
        if (ex != null && ex.getExerciseType() == ExerciseType.FILL_BLANK) {
            if (a.indexOf('(') >= 0 || a.indexOf(')') >= 0) return false; // leaked hint
            if (countGaps(ex.getQuestion()) > 1 && a.indexOf(',') < 0) return false;
        }
        return true;
    }

    /** Detects scraped MC option lines: "[a-e]<space><text>". */
    static final Pattern OPTION_PREFIX = Pattern.compile("(?i)^[a-e]\\s\\S");

    static int countGaps(String question) {
        if (question == null) return 0;
        // Count runs of 2+ consecutive ellipsis (U+2026) or ASCII dots
        int dots = countMatches(question, "(?:\\.{2,}|\\u2026{2,})");
        int us = countMatches(question, "_{2,}");
        return Math.max(dots, us);
    }

    private static int countMatches(String s, String regex) {
        Matcher m = Pattern.compile(regex).matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    static String normalize(String answer) {
        return answer == null ? null : answer.replaceAll("\\s+", " ").trim();
    }

    private static String truncate(String s, int max) {
        return (s == null) ? "" : (s.length() <= max ? s : s.substring(0, max));
    }

    // ── Layer 2: Ollama residue fill ──

    private String askAi(Lesson lesson, Exercise ex) {
        String content = lesson != null ? lesson.getContent() : null;
        String lessonText = (content == null) ? "" : flattenHtml(content);
        if (lessonText.length() > 1500) lessonText = lessonText.substring(0, 1500);
        String typeHint = ex.getExerciseType() == ExerciseType.FILL_BLANK
                ? "FILL_BLANK: fill the gap (use the verb in brackets if given)."
                : "MULTIPLE_CHOICE: give the correct option text.";
        String prompt = String.format(
                "English textbook exercise. Give the correct answer — nothing else.%n"
                + "Lesson: %s%n%s%nQuestion: %s%nAnswer:",
                truncate(lesson != null ? lesson.getTitle() : "", 100),
                typeHint,
                lessonText,
                truncate(ex.getQuestion(), 400));
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String raw = callOllama(prompt);
                String cleaned = sanitizeAiOutput(raw);
                if (cleaned != null && !cleaned.isBlank()) return cleaned;
            } catch (Exception e) {
                log.warn("AI attempt {}/{} failed: {}", attempt + 1, 2, e.getMessage());
            }
        }
        return null;
    }

    private String callOllama(String prompt) {
        WebClient client = WebClient.builder()
                .baseUrl(ollamaBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        try {
            String body = String.format(
                    "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":%s}],\"temperature\":0.5,\"stream\":false}",
                    model,
                    objectMapper.valueToTree(prompt).toString());
            String resp = client.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            JsonNode node = objectMapper.readTree(resp);
            JsonNode choices = node.path("choices");
            if (choices.isArray() && choices.size() > 0)
                return choices.get(0).path("message").path("content").asText();
            throw new RuntimeException("Ollama returned no choices");
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("LLM call failed: " + e.getMessage(), e); }
    }

    /** Strips fences, labels and extra whitespace from model output. */
    static String sanitizeAiOutput(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.startsWith("```")) t = t.replaceFirst("```json", "").replaceFirst("```", "").trim();
        t = t.replaceFirst("(?i)^(answer|correct answer|đáp án)\\s*[:\\-]\\s*", "");
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\""))
            t = t.substring(1, t.length() - 1).trim();
        String[] lines = t.split("\\r?\\n");
        for (String l : lines) { if (!l.isBlank()) return l.trim(); }
        return null;
    }

    @PreDestroy
    void shutdown() {
        pool.shutdown();
        try { if (!pool.awaitTermination(10, TimeUnit.SECONDS)) pool.shutdownNow(); }
        catch (InterruptedException e) { pool.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
