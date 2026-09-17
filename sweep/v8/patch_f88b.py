import sys

def patch(path, pairs, eol):
    with open(path, "r", encoding="utf-8", newline="") as f:
        t = f.read()
    for old, new in pairs:
        o = old.replace("\n", eol); n = new.replace("\n", eol)
        if o not in t:
            print("NOT FOUND in", path, "::", old[:90].replace("\n", "\\n")); sys.exit(1)
        t = t.replace(o, n, 1)
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(t)
    print("patched", path)

CRLF = "\r\n"

patch("src/main/java/com/datn/engflow/service/LessonService.java", [(
"""    public LessonResponse getLessonDetails(Long lessonId, String userEmail) {
        log.info("L\u1ea5y chi ti\u1ebft b\u00e0i h\u1ecdc: id={}, user={}", lessonId, userEmail);

        Lesson lesson = lessonRepository.findByIdWithDetails(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));""",
"""    public LessonResponse getLessonDetails(Long lessonId, String userEmail, boolean requesterIsAdmin) {
        log.info("L\u1ea5y chi ti\u1ebft b\u00e0i h\u1ecdc: id={}, user={}", lessonId, userEmail);

        Lesson lesson = lessonRepository.findByIdWithDetails(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        // audit-v8 F88: detail la endpoint permitAll con list thi da loc isPublished=true — ban
        // nhap (is_published=false) phai 404 voi guest/student, chi admin duoc doc de preview.
        assertVisible(lesson, requesterIsAdmin);"""),
(
"""    @Transactional
    public LessonResponse getLessonDetails(""",
"""    /**
     * audit-v8 F88: chan doc noi dung nhap (is_published=false) qua cac endpoint public.
     * Nem 404 thay vi 403 de khong tiet lo su ton tai cua ban nhap.
     *
     * @param lessonId id bai hoc
     * @param requesterIsAdmin true thi bo qua guard (admin can preview/review ban nhap)
     * @throws ResourceNotFoundException bai hoc khong ton tai, hoac la ban nhap voi nguoi thuong
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

patch("src/main/java/com/datn/engflow/controller/LessonController.java", [(
"""        String email = authentication != null ? authentication.getName() : null;
        LessonResponse lesson = lessonService.getLessonDetails(id, email);""",
"""        String email = authentication != null ? authentication.getName() : null;
        // audit-v8 F88: admin duoc doc ban nhap qua endpoint public de preview.
        boolean isAdmin = authentication != null && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        LessonResponse lesson = lessonService.getLessonDetails(id, email, isAdmin);""")], CRLF)

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
        return authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** True only for an authenticated, non-anonymous principal. */""")], CRLF)
print("ALL PATCHED")
