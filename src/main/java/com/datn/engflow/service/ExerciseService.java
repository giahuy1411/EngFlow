package com.datn.engflow.service;

import com.datn.engflow.exception.BadRequestException;
import com.datn.engflow.model.dto.request.ExerciseRequest;
import com.datn.engflow.model.dto.request.GradeRequest;
import com.datn.engflow.model.dto.response.*;
import com.datn.engflow.model.entity.Exercise;
import com.datn.engflow.model.entity.ExerciseAttempt;
import com.datn.engflow.model.entity.Lesson;
import com.datn.engflow.model.entity.User;
import com.datn.engflow.model.enums.ExerciseDifficulty;
import com.datn.engflow.model.enums.ExerciseType;
import com.datn.engflow.repository.ExerciseAttemptRepository;
import com.datn.engflow.repository.ExerciseRepository;
import com.datn.engflow.repository.LessonRepository;
import com.datn.engflow.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.datn.engflow.model.dto.projection.LessonTitle;
import com.datn.engflow.model.dto.projection.ExerciseLessonProjection;
import com.datn.engflow.service.LessonContentService.LessonContentInfo;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Nghiệp vụ bài tập: đọc/CRUD cho admin, chấm điểm bài nộp của học sinh và lưu lịch sử làm bài.
 *
 * <p>Tầng service, được gọi từ {@code ExerciseController}, {@code AdminExerciseController} và
 * {@code LessonController}. Truy cập dữ liệu qua {@link ExerciseRepository},
 * {@link LessonRepository}, {@link ExerciseAttemptRepository}, {@link UserRepository}; dọn nội
 * dung bài học qua {@link LessonContentService}; ghi ngày học cho streak qua
 * {@link StudyActivityService}.
 *
 * <p>Điểm cần lưu ý: các đường đọc danh sách dùng projection (không kéo cột LOB của lesson),
 * và bài MATCHING có luật chấm riêng dựa trên {@code options} chứ không dùng {@code correct_answer}.
 */
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final LessonRepository lessonRepository;
    private final ExerciseAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final LessonContentService lessonContentService;
    private final StudyActivityService studyActivityService;
    /** audit-v10 F127: doc {@code options} cua bai MATCHING (JSON array). */
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // --- CRUD ---

    /**
     * Danh sách bài tập của một bài học, có thể ẩn đáp án.
     *
     * <p>Dùng projection phẳng để không kéo {@code lesson.content}/{@code content_original}
     * (LOB) theo từng dòng.
     *
     * @param lessonId id bài học
     * @param includeAnswers true để trả cả correctAnswer/explanation (đường admin)
     * @return danh sách bài tập theo thứ tự orderIndex
     */
    public List<ExerciseResponse> getExercisesByLesson(Long lessonId, boolean includeAnswers) {
        // audit-v9 F108: read path only. The JOIN FETCH entity variant is kept for
        // the grading path (it needs managed entities, not a read model) but the
        // list must not carry lesson.content/content_original per row.
        return exerciseRepository.findLessonExercisesProjection(lessonId).stream()
                .map(p -> toResponse(p, includeAnswers))
                .toList();
    }

    /** audit-v9 F108: mapping from the flat list projection (no lesson LOBs). */
    /**
     * Map projection danh sách sang DTO; đáp án bị null hóa khi {@code includeAnswers} false.
     *
     * @param p projection một dòng bài tập (không kèm LOB của lesson)
     * @param includeAnswers true để giữ correctAnswer/explanation
     * @return DTO bài tập
     */
    private ExerciseResponse toResponse(ExerciseLessonProjection p, boolean includeAnswers) {
        return ExerciseResponse.builder()
                .id(p.getId())
                .lessonId(p.getLessonId())
                .lessonTitle(p.getLessonTitle())
                .question(p.getQuestion())
                .options(p.getOptions())
                .correctAnswer(includeAnswers ? p.getCorrectAnswer() : null)
                .exerciseType(p.getExerciseType() != null ? p.getExerciseType().name() : null)
                .difficulty(p.getDifficulty() != null ? p.getDifficulty().name() : null)
                .explanation(includeAnswers ? p.getExplanation() : null)
                .imageUrl(p.getImageUrl())
                .audioUrl(p.getAudioUrl())
                .orderIndex(p.getOrderIndex())
                .build();
    }

    /** Nạp entity bài tập của một bài học (đường chấm điểm cần entity managed). */
    private List<Exercise> findExercisesForLesson(Long lessonId) {
        return exerciseRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
    }

    /**
     * Đọc một bài tập kèm đáp án cho màn hình sửa của admin.
     *
     * @param id id bài tập
     * @return DTO bài tập có correctAnswer/explanation
     * @throws EntityNotFoundException nếu không tìm thấy bài tập
     */
    public ExerciseResponse getExercise(Long id) {
        Exercise ex = exerciseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Exercise not found: " + id));
        // Admin-only endpoint: include correctAnswer/explanation for admin editing.
        return toResponse(ex, true);
    }

    /**
     * Tạo bài tập mới cho một bài học.
     *
     * <p>Với bài MULTIPLE_CHOICE mới, {@code options} phải có nội dung thật (xem
     * {@link #assertNewChoiceOptionsUsable}); đáp án được trả về cho admin vừa tạo.
     *
     * @param request dữ liệu bài tập
     * @return DTO bài tập đã lưu (kèm đáp án)
     * @throws EntityNotFoundException nếu lessonId không tồn tại
     * @throws BadRequestException nếu options của bài MULTIPLE_CHOICE không dùng được
     */
    @Transactional
    public ExerciseResponse createExercise(ExerciseRequest request) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found: " + request.getLessonId()));

        assertNewChoiceOptionsUsable(request.getExerciseType(), request.getOptions());

        Exercise exercise = Exercise.builder()
                .lesson(lesson)
                .question(request.getQuestion())
                .options(request.getOptions())
                .correctAnswer(request.getCorrectAnswer())
                .exerciseType(ExerciseType.valueOf(request.getExerciseType()))
                .difficulty(request.getDifficulty() != null
                        ? ExerciseDifficulty.valueOf(request.getDifficulty()) : null)
                .explanation(request.getExplanation())
                .imageUrl(request.getImageUrl())
                .audioUrl(request.getAudioUrl())
                .orderIndex(request.getOrderIndex())
                .build();

        exercise = exerciseRepository.save(exercise);
        // F7-BUG01 FIX: Return correctAnswer/explanation to the admin who just created it.
        return toResponse(exercise, true);
    }

    /**
     * Cập nhật bài tập theo kiểu patch: chỉ trường khác null trong request mới được ghi đè.
     *
     * <p>Validation options chỉ chạy khi request CÓ gửi options, và xét theo type mà dòng sẽ
     * mang sau khi lưu — cố ý không kiểm dòng cũ, vì hàng loạt bài MULTIPLE_CHOICE legacy có
     * {@code options IS NULL} sẽ bị chặn sửa oan.
     *
     * @param id id bài tập cần sửa
     * @param request dữ liệu mới (trường null = giữ nguyên)
     * @return DTO bài tập đã lưu (kèm đáp án)
     * @throws EntityNotFoundException nếu không tìm thấy bài tập
     * @throws BadRequestException nếu options gửi lên không dùng được cho type hiệu lực
     */
    @Transactional
    public ExerciseResponse updateExercise(Long id, ExerciseRequest request) {
        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Exercise not found: " + id));

        // audit-v13 F-13-01: validate ONLY when this update supplies options, and judge
        // the type the row will END UP with. Deliberately NOT validating the stored row
        // when the request omits options: 32,814 legacy MULTIPLE_CHOICE rows have
        // options IS NULL, and validating them here would block every legitimate edit
        // (question/explanation/difficulty) of those rows.
        if (request.getOptions() != null) {
            String effectiveType = request.getExerciseType() != null
                    ? request.getExerciseType()
                    : (exercise.getExerciseType() != null ? exercise.getExerciseType().name() : null);
            assertNewChoiceOptionsUsable(effectiveType, request.getOptions());
        }

        if (request.getQuestion() != null) exercise.setQuestion(request.getQuestion());
        if (request.getOptions() != null) exercise.setOptions(request.getOptions());
        if (request.getCorrectAnswer() != null) exercise.setCorrectAnswer(request.getCorrectAnswer());
        if (request.getExerciseType() != null) exercise.setExerciseType(ExerciseType.valueOf(request.getExerciseType()));
        if (request.getDifficulty() != null) exercise.setDifficulty(ExerciseDifficulty.valueOf(request.getDifficulty()));
        if (request.getExplanation() != null) exercise.setExplanation(request.getExplanation());
        if (request.getImageUrl() != null) exercise.setImageUrl(request.getImageUrl());
        if (request.getAudioUrl() != null) exercise.setAudioUrl(request.getAudioUrl());
        if (request.getOrderIndex() != null) exercise.setOrderIndex(request.getOrderIndex());

        exercise = exerciseRepository.save(exercise);
        // F7-BUG01 FIX: Admin update should also return the answer so the editor sees what changed.
        return toResponse(exercise, true);
    }

    /**
     * Xóa một bài tập theo id.
     *
     * @param id id bài tập cần xóa
     * @throws EntityNotFoundException nếu không tìm thấy bài tập
     */
    @Transactional
    public void deleteExercise(Long id) {
        if (!exerciseRepository.existsById(id)) {
            throw new EntityNotFoundException("Exercise not found: " + id);
        }
        exerciseRepository.deleteById(id);
    }

    /**
     * audit-v13 F-13-01: a NEW choice-type exercise must carry usable answer text.
     *
     * <p>Context measured in the live DB (2026-09-22): of 33,556 MULTIPLE_CHOICE rows,
     * 32,814 have {@code options IS NULL} and 0 have bare-letter options. Those legacy
     * NULL rows are answered by typing the answer and are graded server-side, so this
     * guard deliberately validates only what an admin is CREATING — never the stored
     * shape of an existing row, or every legacy edit would start failing.
     *
     * @throws BadRequestException when a new choice item would be persisted unusable
     */
    private void assertNewChoiceOptionsUsable(String exerciseType, String options) {
        if (exerciseType == null
                || !ExerciseType.MULTIPLE_CHOICE.name().equalsIgnoreCase(exerciseType.trim())) {
            return;
        }
        List<String> opts = parseOptionsList(options);
        if (opts.size() < 2) {
            throw new BadRequestException(
                    "MULTIPLE_CHOICE cần ít nhất 2 lựa chọn có nội dung thật");
        }
        // Reject when the options carry no answer text at all: every entry is either a
        // bare letter ("a") or blank. Mixed input like ["a","b","c","Hanoi"] is allowed
        // because it does contain usable content.
        boolean noRealContent = opts.stream().allMatch(this::isBareLetterOption);
        if (noRealContent) {
            throw new BadRequestException(
                    "MULTIPLE_CHOICE cần nội dung lựa chọn thật, không chỉ \"a\"/\"b\"/\"c\"/\"d\"");
        }
    }

    /**
     * True when an option is only a bare option letter placeholder — "a", "B", or a
     * letter with nothing after it.
     *
     * <p>Deliberately does NOT treat {@code "A - Salad"} as a placeholder: that form
     * carries real answer content (real row {@code exercise_id=651717}, options
     * {@code ["A - Salad","B - Cheeseburger",...]}). The old regex
     * {@code ^[A-D](\s*-\s*.*)?$} matched it, which would have wrongly rejected a
     * legitimate exercise.
     */
    private boolean isBareLetterOption(String option) {
        return option != null && option.trim().matches("(?i)^[a-d]$");
    }

    /** Parses a JSON options array, returning an empty list for null/blank/invalid input. */
    private List<String> parseOptionsList(String options) {
        if (options == null || options.isBlank()) {
            return List.of();
        }
        try {
            // Fall back to a local mapper when none is injected (unit-test construction).
            // A missing dependency must never turn into a false rejection of valid input.
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    objectMapper != null ? objectMapper : new com.fasterxml.jackson.databind.ObjectMapper();
            List<String> parsed = mapper.readValue(options, new TypeReference<List<String>>() {});
            return parsed.stream()
                    .filter(o -> o != null && !o.trim().isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    // --- Grading ---

    /**
     * Chấm điểm một lượt làm bài nhưng KHÔNG lưu lịch sử — dùng cho luồng xem trước/kiểm tra.
     *
     * <p>Nạp entity bài tập qua {@link #findExercisesForLesson} (đường cần entity managed), rồi so
     * khớp từng câu trả lời. Hai loại bài bị coi là <b>không chấm được</b> ({@code ungradeable=true})
     * và bị loại khỏi CẢ tử số lẫn mẫu số (không tính là sai):
     * <ul>
     *   <li>bài thiếu {@code correct_answer} (rỗng) — so {@code ""} với {@code ""} sẽ báo đúng oan;</li>
     *   <li>bài MATCHING có {@code options} hỏng, không sinh ra cặp nào (audit-v10 F127).</li>
     * </ul>
     * {@code percentage} tính trên mẫu số {@code total} (số câu chấm được), làm tròn 2 chữ số.
     *
     * @param lessonId id bài học
     * @param request  danh sách câu trả lời; nếu {@code answers == null} trả về kết quả rỗng 0/0
     * @return kết quả chấm (từng câu + điểm + tổng + phần trăm)
     */
    @Transactional
    public GradeResponse gradeExercises(Long lessonId, GradeRequest request) {
        List<Exercise> exercises = findExercisesForLesson(lessonId);
        List<ExerciseGradeItem> results = new ArrayList<>();
        int score = 0;
        int total = 0;

        if (request.getAnswers() == null) {
            return GradeResponse.builder()
                    .results(results)
                    .score(0)
                    .total(0)
                    .percentage(0)
                    .build();
        }

        for (GradeRequest.AnswerItem item : request.getAnswers()) {
            if (item.getExerciseId() == null) continue;

            Exercise ex = exercises.stream()
                    .filter(e -> e.getId().equals(item.getExerciseId()))
                    .findFirst().orElse(null);

            if (ex == null) continue;

            // Ungradeable: exercise has a null/blank answer key. Comparing "" to ""
            // would mark an empty user answer as correct (false-positive), so the item
            // is excluded from the score/total denominator instead.
            // audit-v10 F127: MATCHING co nguon dap an rieng (options), nen no
            // khong chiu rang buoc correct_answer. Mot bai MATCHING voi options
            // hong cung phai duoc coi la khong cham duoc, dung nhu bai thieu
            // correct_answer — neu khong se bi tinh la SAI thay vi bi loai.
            boolean ungradeable = (ex.getCorrectAnswer() == null || ex.getCorrectAnswer().isBlank())
                    || isMatchingUngradeable(ex);
            boolean correct = false;
            if (ungradeable) {
                results.add(ExerciseGradeItem.builder()
                        .exerciseId(ex.getId())
                        .correct(false)
                        .ungradeable(true)
                        .userAnswer(item.getUserAnswer())
                        .correctAnswer(ex.getCorrectAnswer())
                        .build());
                continue;
            }

            correct = isCorrectAnswer(ex, item.getUserAnswer());
            if (correct) score++;
            total++;

            results.add(ExerciseGradeItem.builder()
                    .exerciseId(ex.getId())
                    .correct(correct)
                    .userAnswer(item.getUserAnswer())
                    .correctAnswer(ex.getCorrectAnswer())
                    .build());
        }

        double pct = total == 0 ? 0 :
                (double) score / total * 100;

        return GradeResponse.builder()
                .results(results)
                .score(score)
                .total(total)
                .percentage(Math.round(pct * 100.0) / 100.0)
                .build();
    }

    /**
     * So khớp câu trả lời cho một bài tập.
     *
     * <p><b>audit-v10 F127:</b> MATCHING KHÔNG được chấm bằng so khớp chuỗi với
     * {@code correct_answer}. Hai lý do đo được, không phải suy đoán:
     *
     * <ol>
     *   <li><b>Không khớp định dạng.</b> Đo 331 row MATCHING trong bài đã
     *       published: 330 row lưu {@code correct_answer} dạng CHỮ
     *       ({@code A=B,B=D,C=C,D=A} hoặc {@code word1=be,...}), chỉ 1 row dạng
     *       chỉ số. Nhưng client gửi lên dạng CHỈ SỐ VỊ TRÍ
     *       ({@code 0=0,1=1,2=2,3=3}). Thử 4 bài published, gửi đúng định dạng
     *       client gửi: <b>cả 4 đều {@code correct=false}</b>, trong khi gửi
     *       đúng chuỗi thô thì {@code true}. Tức học sinh nối đúng hết vẫn 0 điểm.</li>
     *   <li><b>Chỉ số vô nghĩa với server.</b> {@code MatchingExercise.vue} XÁO
     *       TRỘN cột phải (dòng 157-166), nên chỉ số hiển thị mà client gửi
     *       KHÔNG bằng chỉ số gốc. Server không có cách nào dựng lại phép hoán
     *       vị đó. Vì vậy sửa định dạng chỉ số là bất khả thi về nguyên tắc —
     *       client phải gửi CHỮ.</li>
     * </ol>
     *
     * <p>Cách chấm đúng: cặp đúng lấy từ {@code options} (mỗi phần tử là
     * {@code "left|right"}), đối chiếu với tập cặp CHỮ mà client gửi lên, so
     * khớp theo <b>tập hợp</b> nên thứ tự nối không ảnh hưởng kết quả.
     *
     * <p>{@code correct_answer} không dùng để chấm MATCHING nữa. Đo được nó
     * lệch với {@code options} ở 96 row, và còn 19 row giữ nguyên placeholder
     * {@code wordN=defN} chưa từng được điền — dùng nó làm nguồn sự thật sẽ
     * chấm sai.
     */
    private boolean isCorrectAnswer(Exercise ex, String userAnswer) {
        if (ex.getExerciseType() == ExerciseType.MATCHING) {
            return matchingPairsMatch(userAnswer, ex.getOptions());
        }
        return normalizeAnswer(userAnswer).equals(normalizeAnswer(ex.getCorrectAnswer()));
    }

    /**
     * True khi tập cặp người học nối BẰNG ĐÚNG tập cặp hợp lệ lấy từ options.
     *
     * <p>Không chấp nhận tập con: nối đúng 2/4 cặp là chưa hoàn thành bài.
     */
    private boolean matchingPairsMatch(String userAnswer, String optionsJson) {
        Set<String> correct = pairsFromOptions(optionsJson);
        if (correct.isEmpty()) return false;
        return parsePairs(userAnswer).equals(correct);
    }

    /**
     * Cặp hợp lệ lấy từ {@code options} — mỗi phần tử là {@code "left|right"}.
     *
     * <p>Trả về rỗng khi options không phải JSON mảng hoặc không phần tử nào có
     * dấu {@code |}; khi đó bài tập được coi là KHÔNG CHẤM ĐƯỢC (xem
     * {@link #isMatchingUngradeable}) thay vì âm thầm cho 0 điểm.
     */
    private Set<String> pairsFromOptions(String optionsJson) {
        Set<String> pairs = new HashSet<>();
        if (optionsJson == null || optionsJson.isBlank()) return pairs;
        List<String> opts;
        try {
            opts = objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception notAJsonArray) {
            return pairs;
        }
        if (opts == null) return pairs;
        for (String opt : opts) {
            if (opt == null) continue;
            int bar = opt.indexOf('|');
            if (bar <= 0) continue;
            String left = normalizePairSide(opt.substring(0, bar));
            String right = normalizePairSide(opt.substring(bar + 1));
            if (!left.isEmpty() && !right.isEmpty()) pairs.add(left + "=" + right);
        }
        return pairs;
    }

    /** Tách chuỗi "X=Y,X=Y" client gửi thành tập cặp đã chuẩn hoá. */
    private Set<String> parsePairs(String raw) {
        Set<String> pairs = new HashSet<>();
        if (raw == null || raw.isBlank()) return pairs;
        for (String part : raw.split(",")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String left = normalizePairSide(part.substring(0, eq));
            String right = normalizePairSide(part.substring(eq + 1));
            if (!left.isEmpty() && !right.isEmpty()) pairs.add(left + "=" + right);
        }
        return pairs;
    }

    /** Bỏ nháy/khoảng trắng thừa, hạ chữ thường, để "A = D" và "a=d" là một. */
    private String normalizePairSide(String s) {
        return s == null ? "" : s.trim().replaceAll("^['\"]|['\"]$", "").trim().toLowerCase();
    }

    /**
     * Bài MATCHING không chấm được vì options không sinh ra cặp nào.
     *
     * <p>Cùng chính sách với {@code correct_answer} rỗng: loại khỏi tử số VÀ mẫu
     * số, thay vì tính là sai. Đo được 11 row như vậy trong bài đã published.
     */
    private boolean isMatchingUngradeable(Exercise ex) {
        return ex.getExerciseType() == ExerciseType.MATCHING
                && pairsFromOptions(ex.getOptions()).isEmpty();
    }

    private String normalizeAnswer(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    // audit-v7 C-03a: method getAllExercises(lessonId,...) với nhánh
    // exerciseRepository.findAll() (43.7k dòng, 2.3k reads/exec, query chậm nhất
    // app đo được 308ms) đã BỊ XÓA — controller duy nhất dùng bài tập là
    // AdminExerciseController vốn gọi getAdminExercisePage (paginated, push
    // filter xuống SQL qua findAdminPage). Giữ lại đường không phân trang là
    // tái introducing worst-scaling-query.

    /**
     * Trang danh sách bài tập cho admin, có filter lesson/type/difficulty/search.
     *
     * <p><b>Tối ưu hiệu năng:</b> dùng {@code findAdminPage} đẩy filter xuống SQL (phân trang), và
     * KHÔNG hydrate entity Lesson theo từng dòng — tên lesson lấy bằng một query batch riêng
     * ({@code findTitlesById}) để tránh kéo hai cột {@code NVARCHAR(MAX)} của lesson
     * (audit-v8 perf: từng ~95k logical reads/trang). Lưu ý {@code search} là LIKE {@code %kw%}.
     *
     * @param lessonId   filter theo lesson (null = tất cả)
     * @param type       filter theo loại bài tập (chuỗi, null = tất cả)
     * @param difficulty filter theo độ khó (chuỗi, null = tất cả)
     * @param search     từ khóa tìm trong câu hỏi (null/rỗng = bỏ qua)
     * @param pageable   thông tin phân trang
     * @return trang DTO bài tập cho admin
     */
    public Page<ExerciseResponse> getAdminExercisePage(Long lessonId, String type, String difficulty, String search, Pageable pageable) {
        ExerciseType exerciseType = parseEnum(type, ExerciseType.class);
        ExerciseDifficulty exerciseDifficulty = parseEnum(difficulty, ExerciseDifficulty.class);
        Page<Exercise> page = exerciseRepository.findAdminPage(
                lessonId,
                exerciseType,
                exerciseDifficulty,
                search != null && !search.isBlank() ? search.trim() : null,
                pageable);
        // audit-v8 perf: read the row labels straight from the lesson table as id+title.
        // Hydrating the joined Lesson entity pulled content/content_original (NVARCHAR MAX)
        // for every row on the page, which was the single most read-heavy statement the app
        // issues (95k logical reads per page). One extra batched query replaces that.
        java.util.Set<Long> lessonIds = page.getContent().stream()
                .map(e -> e.getLesson() == null ? null : e.getLesson().getId())
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> titles = lessonIds.isEmpty() ? Map.of()
                : lessonRepository.findTitlesById(lessonIds).stream()
                        .collect(Collectors.toMap(LessonTitle::getLessonId, t -> t.getTitle() == null ? "" : t.getTitle(), (a, b) -> a));
        return page.map(e -> toAdminRow(e, titles));
    }

    /**
     * Admin row for the paginated list. Built from scalar exercise columns only: reading
     * {@code ex.getLesson().getTitle()} would initialise the lazy Lesson proxy and pull the
     * two NVARCHAR(MAX) columns for every row, which is exactly the cost this page must avoid.
     * The lesson id comes from the proxy without a select; the title is looked up once per page.
     */
    private ExerciseResponse toAdminRow(Exercise ex, Map<Long, String> titles) {
        Long lid = ex.getLesson() == null ? null : ex.getLesson().getId();
        return ExerciseResponse.builder()
                .id(ex.getId())
                .lessonId(lid)
                .lessonTitle(lid == null ? null : titles.get(lid))
                .question(ex.getQuestion())
                .options(ex.getOptions())
                .correctAnswer(ex.getCorrectAnswer())
                .exerciseType(ex.getExerciseType().name())
                .difficulty(ex.getDifficulty() != null ? ex.getDifficulty().name() : null)
                .explanation(ex.getExplanation())
                .imageUrl(ex.getImageUrl())
                .audioUrl(ex.getAudioUrl())
                .orderIndex(ex.getOrderIndex())
                .build();
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass) {
        if (value == null || value.isBlank()) return null;
        return Enum.valueOf(enumClass, value.trim().toUpperCase());
    }

    // --- Exercise Attempts ---

    /**
     * Nộp bài của học sinh: chấm điểm rồi lưu lịch sử làm bài và ghi nhận ngày học cho streak.
     *
     * <p>Luồng: gọi {@link #gradeExercises} → tra user theo email → nạp một lượt các entity bài tập
     * theo id ({@code findAllById}, tránh N+1) → dựng {@code detailsJson} (câu hỏi, đáp án người
     * dùng, đáp án đúng, đúng/sai, giải thích) → lưu {@link ExerciseAttempt} kèm thời điểm
     * {@code LocalDateTime.now()} (giờ VN naive, xem quy ước timezone ở AGENTS.md).
     *
     * <p><b>Ghi {@code study_days}:</b> chỉ gọi {@link StudyActivityService#recordStudy} khi có ÍT
     * NHẤT một câu trả lời không rỗng — nộp bài trắng không tính là một ngày học. Lưu ý guard draft
     * lesson nằm ở tầng controller ({@code LessonController}) chặn học viên nộp bài vào lesson chưa
     * publish, nên service này không kiểm lại trạng thái publish.
     *
     * @param lessonId  id bài học
     * @param request   danh sách câu trả lời
     * @param userEmail email người nộp (lấy từ JWT)
     * @return kết quả chấm điểm
     * @throws EntityNotFoundException nếu không tìm thấy user
     */
    @Transactional
    public GradeResponse submitExercises(Long lessonId, GradeRequest request, String userEmail) {
        GradeResponse grade = gradeExercises(lessonId, request);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userEmail));

        // Batch fetch all exercises for detailsJson building to avoid N+1 query
        List<Long> exerciseIds = grade.getResults().stream()
                .map(ExerciseGradeItem::getExerciseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Exercise> exerciseMap = exerciseRepository.findAllById(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));

        // Build details JSON
        String detailsJson = grade.getResults().stream()
                .map(item -> {
                    Exercise ex = exerciseMap.get(item.getExerciseId());
                    return "{\"exerciseId\":" + item.getExerciseId()
                            + ",\"question\":" + (ex != null ? escapeJson(ex.getQuestion()) : "null")
                            + ",\"userAnswer\":" + escapeJson(item.getUserAnswer())
                            + ",\"correctAnswer\":" + escapeJson(item.getCorrectAnswer())
                            + ",\"isCorrect\":" + item.isCorrect()
                            + ",\"explanation\":" + (ex != null ? escapeJson(ex.getExplanation()) : "null")
                            + "}";
                })
                .collect(Collectors.joining(",", "[", "]"));

        BigDecimal pct = BigDecimal.valueOf(grade.getPercentage())
                .setScale(2, RoundingMode.HALF_UP);

        ExerciseAttempt attempt = ExerciseAttempt.builder()
                .user(user)
                .lessonId(lessonId)
                .score(grade.getScore())
                .total(grade.getTotal())
                .percentage(pct)
                .details(detailsJson)
                .completedAt(LocalDateTime.now())
                .build();

        attemptRepository.save(attempt);

        if (grade.getResults().stream().anyMatch(item ->
                item.getUserAnswer() != null && !item.getUserAnswer().isBlank())) {
            studyActivityService.recordStudy(user.getId());
        }

        return grade;
    }

    private String escapeJson(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    /**
     * Lịch sử các lượt nộp bài của một user cho một lesson, mới nhất trước.
     *
     * @param lessonId  id bài học
     * @param userEmail email người học
     * @return danh sách tóm tắt lượt làm (điểm, tổng, phần trăm, thời điểm)
     * @throws EntityNotFoundException nếu không tìm thấy user
     */
    public List<AttemptHistoryResponse> getAttemptHistory(Long lessonId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userEmail));
        return attemptRepository.findByUserIdAndLessonIdOrderByCompletedAtDesc(user.getId(), lessonId)
                .stream()
                .map(a -> AttemptHistoryResponse.builder()
                        .id(a.getId())
                        .score(a.getScore())
                        .total(a.getTotal())
                        .percentage(a.getPercentage())
                        .completedAt(a.getCompletedAt())
                        .build())
                .toList();
    }

    /**
     * Chi tiết một lượt làm bài (từng câu, đáp án người dùng, đáp án đúng, đúng/sai, giải thích).
     *
     * <p><b>Chống IDOR:</b> lượt làm được tra bằng {@code findByIdAndUserId} nên user chỉ xem được
     * lượt của chính mình, không xem được lượt của người khác dù biết {@code attemptId}.
     *
     * <p>Cột {@code details} chứa JSON mảng các object được dựng thủ công ở {@link #submitExercises};
     * ở đây parse bằng tay (không qua Jackson) vì tin định dạng do chính app ghi ra.
     *
     * @param lessonId  id bài học
     * @param attemptId id lượt làm
     * @param userEmail email người học (lấy từ JWT)
     * @return chi tiết lượt làm
     * @throws EntityNotFoundException nếu không tìm thấy user hoặc lượt làm không thuộc user này
     */
    public AttemptDetailResponse getAttemptDetail(Long lessonId, Long attemptId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userEmail));
        ExerciseAttempt attempt = attemptRepository.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found: " + attemptId));

        // Parse details JSON (simple parse — trust format)
        String raw = attempt.getDetails();
        List<AttemptDetailResponse.AttemptDetailItem> items = new ArrayList<>();
        if (raw != null && raw.startsWith("[")) {
            String itemsStr = raw.substring(1, raw.length() - 1);
            if (!itemsStr.isBlank()) {
                for (String part : splitJsonArray(itemsStr)) {
                    try {
                        Long eId = extractLong(part, "exerciseId");
                        String q = extractString(part, "question");
                        String ua = extractString(part, "userAnswer");
                        String ca = extractString(part, "correctAnswer");
                        boolean ic = extractBoolean(part, "isCorrect");
                        String exp = extractString(part, "explanation");
                        items.add(AttemptDetailResponse.AttemptDetailItem.builder()
                                .exerciseId(eId).question(q).userAnswer(ua)
                                .correctAnswer(ca).isCorrect(ic).explanation(exp)
                                .build());
                    } catch (Exception ignored) {}
                }
            }
        }

        return AttemptDetailResponse.builder()
                .id(attempt.getId())
                .score(attempt.getScore())
                .total(attempt.getTotal())
                .percentage(attempt.getPercentage())
                .completedAt(attempt.getCompletedAt())
                .details(items)
                .build();
    }

    private List<String> splitJsonArray(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') { depth++; if (depth == 1) start = i; }
            else if (c == '}') { depth--; if (depth == 0) parts.add(s.substring(start, i + 1)); }
        }
        return parts;
    }

    private Long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int valStart = idx + search.length();
        int valEnd = valStart;
        while (valEnd < json.length() && Character.isDigit(json.charAt(valEnd))) valEnd++;
        try { return Long.parseLong(json.substring(valStart, valEnd)); } catch (NumberFormatException e) { return null; }
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int valStart = json.indexOf('"', idx + search.length()) + 1;
        if (valStart == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = valStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"') { sb.append('"'); i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else if (next == 'n') { sb.append('\n'); i++; }
                else { sb.append(c); }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean extractBoolean(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return false;
        return json.substring(idx + search.length()).startsWith("true");
    }

    // --- Mappers ---

    private ExerciseResponse toResponse(Exercise ex) {
        return toResponse(ex, false);
    }

    private ExerciseResponse toResponse(Exercise ex, boolean includeAnswers) {
        return ExerciseResponse.builder()
                .id(ex.getId())
                .lessonId(ex.getLesson().getId())
                .lessonTitle(ex.getLesson().getTitle())
                .question(ex.getQuestion())
                .options(ex.getOptions())
                .correctAnswer(includeAnswers ? ex.getCorrectAnswer() : null)
                .exerciseType(ex.getExerciseType().name())
                .difficulty(ex.getDifficulty() != null ? ex.getDifficulty().name() : null)
                .explanation(includeAnswers ? ex.getExplanation() : null)
                .imageUrl(ex.getImageUrl())
                .audioUrl(ex.getAudioUrl())
                .orderIndex(ex.getOrderIndex())
                .build();
    }

    /**
     * Nội dung bài học đã được làm sạch/làm gọn để hiển thị cho người học.
     *
     * <p>Nội dung trong DB hầu hết đã được xử lý sẵn, nhưng vẫn chạy một lượt
     * {@link LessonContentService#wrapAnswerSections} ở runtime để bọc các khối "ĐÁP ÁN" thành dạng
     * thu gọn — kể cả 58 lesson cũ được tạo trước bộ chuyển đổi offline (audit-v7 F53).
     *
     * @param lessonId id bài học
     * @return thông tin nội dung đã làm sạch
     * @throws EntityNotFoundException nếu không tìm thấy lesson
     */
    public LessonContentInfo getCleanContent(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found: " + lessonId));
        // audit-v7 F53: DB content is mostly pre-cleaned with <details> for answers,
        // but a runtime pass makes visible "ĐÁP ÁN" blocks collapsible even for the
        // 58 lessons that predate the offline converter.
        return new LessonContentInfo(lessonContentService.wrapAnswerSections(lesson.getContent()));
    }
}
