import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

LR = "src/main/java/com/datn/engflow/repository/LessonRepository.java"
ES = "src/main/java/com/datn/engflow/service/ExerciseService.java"

lr = open(LR, encoding="utf-8").read()
if "findTitlesById" not in lr:
    anchor = "    List<Lesson> findByTitleContainingIgnoreCase(String keyword);"
    assert lr.count(anchor) == 1
    add = anchor + """

    /**
     * audit-v8 perf: id + title only, for labelling the admin exercise page. Selecting the
     * projection keeps Hibernate from hydrating content/content_original (NVARCHAR MAX),
     * which the joined Lesson entity did on every row of the page.
     */
    @Query("SELECT l.id AS lessonId, l.title AS title FROM Lesson l WHERE l.id IN :ids")
    java.util.List<com.datn.engflow.model.dto.projection.LessonTitle> findTitlesById(
            @Param("ids") java.util.Collection<Long> ids);"""
    open(LR, "w", encoding="utf-8", newline="").write(lr.replace(anchor, add))
    print("repo method added")
else:
    print("repo method present")

es = open(ES, encoding="utf-8").read()
old = """    private ExerciseResponse toAdminRow(Exercise ex, Map<Long, String> titles) {
        ExerciseResponse base = toResponse(ex, true);
        if (!titles.isEmpty() && ex.getLesson() != null) {
            base.setLessonTitle(titles.getOrDefault(ex.getLesson().getId(), base.getLessonTitle()));
        }
        return base;
    }"""
i = es.find("    private ExerciseResponse toAdminRow")
j = es.find("\n    }", i) + len("\n    }")
old = es[i:j]
new = """    /**
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
    }"""
assert i > 0
open(ES, "w", encoding="utf-8", newline="").write(es[:i] + new + es[j:])
print("toAdminRow replaced")
