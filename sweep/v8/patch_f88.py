import sys

def patch(path, pairs, eol):
    with open(path, "r", encoding="utf-8", newline="") as f:
        t = f.read()
    for old, new in pairs:
        o = old.replace("\n", eol); n = new.replace("\n", eol)
        if o not in t:
            print("NOT FOUND in", path, "::", old[:80].replace("\n", "\\n")); sys.exit(1)
        t = t.replace(o, n, 1)
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(t)
    print("patched", path)

LF = "\n"; CRLF = "\r\n"

# A) VideoDtos: bo @NotNull tren transcript (null = giu nguyen khi update)
patch("src/main/java/com/datn/engflow/model/dto/video/VideoDtos.java", [(
"            @NotNull List<TranscriptLine> transcript,",
"""            // audit-v8 F88: null = "giu nguyen phu de hien co" khi UPDATE (audit-v6 F21) nen KHONG
            // duoc @NotNull o day — annotation do bien guard null trong service thanh dead code va
            // moi lan admin sua bai (bo trong o phu de) deu 400. CREATE van bi chan boi
            // VideoLessonService.validateTranscript: 400 "Transcript can it nhat 2 dong".
            List<TranscriptLine> transcript,""")], LF)

# B) VideoLessonService.getDetail: nhan co admin + chan doc ban nhap
patch("src/main/java/com/datn/engflow/service/VideoLessonService.java", [(
"""    public VideoLessonDetail getDetail(Long id, Long userId) {
        VideoLesson lesson = getLesson(id);""",
"""    public VideoLessonDetail getDetail(Long id, Long userId, boolean requesterIsAdmin) {
        VideoLesson lesson = getLesson(id);
        // audit-v8 F88: list da loc isPublished=true thi detail phai khop — ban nhap
        // (is_published=false) khong duoc lo cho guest/student; admin van xem duoc de review.
        if (!Boolean.TRUE.equals(lesson.getIsPublished()) && !requesterIsAdmin) {
            throw new ResourceNotFoundException("VideoLesson", "id", id);
        }""")], LF)

# C) VideoLessonController.detail: truyen co admin
patch("src/main/java/com/datn/engflow/controller/video/VideoLessonController.java", [(
"""        return ResponseEntity.ok(videoLessonService.getDetail(id, userPrincipal == null ? null : userPrincipal.getId()));""",
"""        boolean isAdmin = userPrincipal != null && userPrincipal.getAuthorities() != null
                && userPrincipal.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return ResponseEntity.ok(videoLessonService.getDetail(id,
                userPrincipal == null ? null : userPrincipal.getId(), isAdmin));""")], LF)

# D) LessonService: getLessonDetails + assertVisible
patch("src/main/java/com/datn/engflow/service/LessonService.java", [(
"""    public LessonResponse getLessonDetails(Long lessonId, String userEmail) {
        log.info("Lay chi tiet bai hoc: id={}, user={}", lessonId, userEmail);

        Lesson lesson = lessonRepository.findByIdWithDetails(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));""",
"""    public LessonResponse getLessonDetails(Long lessonId, String userEmail, boolean requesterIsAdmin) {
        log.info("Lay chi tiet bai hoc: id={}, user={}", lessonId, userEmail);

        Lesson lesson = lessonRepository.findByIdWithDetails(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        // audit-v8 F88: endpoint detail la permitAll va list da loc isPublished=true — ban nhap
        // phai tra 404 cho guest/student, chi admin duoc doc de review.
        assertVisible(lesson, requesterIsAdmin);"""),
(
"""    @Transactional
    public LessonResponse getLessonDetails(""",
"""    /**
     * audit-v8 F88: chan doc noi dung nhap (is_published=false) qua cac endpoint public.
     * Nem 404 (khong phai 403) de khong tiet lo su ton tai cua ban nhap.
     *
     * @param lessonId id bai hoc
     * @param requesterIsAdmin admin duoc bo qua guard de con preview/review
     * @throws ResourceNotFoundException khi bai hoc khong ton tai hoac la ban nhap voi nguoi thuong
     */
    @Transactional(readOnly = true)
    public void assertLessonVisible(Long lessonId, boolean requesterIsAdmin) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        assertVisible(lesson, requesterIsAdmin);
    }

    private void assertVisible(Lesson lesson, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !Boolean.TRUE.equals(lesson.getIsPublished())) {
            throw new ResourceNotFoundException("Lesson", "id", lesson.getId());
        }
    }

    @Transactional
    public LessonResponse getLessonDetails(""")], CRLF)

# E) LessonController: tinh co admin tu Authentication
patch("src/main/java/com/datn/engflow/controller/LessonController.java", [(
"""        String email = authentication != null ? authentication.getName() : null;
        LessonResponse lesson = lessonService.getLessonDetails(id, email);""",
"""        String email = authentication != null ? authentication.getName() : null;
        // audit-v8 F88: admin duoc doc ban nhap qua endpoint public de preview.
        boolean isAdmin = authentication != null && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        LessonResponse lesson = lessonService.getLessonDetails(id, email, isAdmin);""")], CRLF)

# F) LessonExerciseController: guard 2 endpoint public doc theo lesson
patch("src/main/java/com/datn/engflow/controller/LessonExerciseController.java", [(
"""import com.datn.engflow.service.ExerciseService;
import com.datn.engflow.service.LessonContentService;""",
"""import com.datn.engflow.service.ExerciseService;
import com.datn.engflow.service.LessonContentService;
import com.datn.engflow.service.LessonService;"""),
(
"""    private final ExerciseService exerciseService;
    private final LessonContentService lessonContentService;""",
"""    private final ExerciseService exerciseService;
    private final LessonContentService lessonContentService;
    private final LessonService lessonService;"""),
(
"""            if (!isAdmin) {
                return ResponseEntity.status(403).build();
            }
        }
        List<ExerciseResponse> exercises = exerciseService.getExercisesByLesson(lessonId, includeAnswers);""",
"""            if (!isAdmin) {
                return ResponseEntity.status(403).build();
            }
        }
        // audit-v8 F88: bai nhap khong duoc doc cong khai (admin bo qua de preview).
        lessonService.assertLessonVisible(lessonId, isAdmin(authentication));
        List<ExerciseResponse> exercises = exerciseService.getExercisesByLesson(lessonId, includeAnswers);"""),
(
"""    public ResponseEntity<LessonContentInfo> getCleanContent(
            @PathVariable Long lessonId) {
        LessonContentInfo info = exerciseService.getCleanContent(lessonId);""",
"""    public ResponseEntity<LessonContentInfo> getCleanContent(
            @PathVariable Long lessonId,
            Authentication authentication) {
        // audit-v8 F88: cung guard voi /exercises.
        lessonService.assertLessonVisible(lessonId, isAdmin(authentication));
        LessonContentInfo info = exerciseService.getCleanContent(lessonId);"""),
(
"""    /** True only for an authenticated, non-anonymous principal. */""",
"""    /** True khi principal that su co ROLE_ADMIN (anonymous user khong bao gio co). */
    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String name = authentication.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return false;
        }
        // includeAnswers da duoc guard 403 o tren; o day chi can biet co phai admin hay khong.
        return authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** True only for an authenticated, non-anonymous principal. */""")], CRLF)
print("ALL PATCHED")
