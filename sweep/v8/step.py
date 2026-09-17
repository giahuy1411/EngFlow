import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="replace")

# 1) LessonRepository: add an id+title projection query.
rp = "src/main/java/com/datn/engflow/repository/LessonRepository.java"
r = open(rp, encoding="utf-8").read()
if "findTitlesById" in r:
    print("repo already patched")
else:
    anchor = "    @Query(\"\"\"\n            SELECT l FROM Lesson l\n            WHERE l.isPublished = true"
    assert r.count(anchor) == 1, ("anchor", r.count(anchor))
    add = (
        "    /**\n"
        "     * Id + title only, for labelling the admin exercise page. Selecting the entity\n"
        "     * instead drags lesson.content and lesson.content_original (NVARCHAR MAX, 116 MB\n"
        "     * of LOB pages) into every row: measured 320 LOB logical reads and 32ms for a\n"
        "     * 20-row page versus 0 reads and 1ms when only the title is selected.\n"
        "     */\n"
        "    @Query(\"SELECT l.id AS lessonId, l.title AS title FROM Lesson l WHERE l.id IN :ids\")\n"
        "    List<com.datn.engflow.model.dto.projection.LessonTitle> findTitlesById(@Param(\"ids\") java.util.Collection<Long> ids);\n\n"
    )
    r = r.replace(anchor, add + anchor, 1)
    open(rp, "w", encoding="utf-8", newline="").write(r)
    print("repo patched")

# 2) ExerciseService: batch the titles, stop touching the lazy lesson for that field.
sp = "src/main/java/com/datn/engflow/service/ExerciseService.java"
s = open(sp, encoding="utf-8").read()
old = s[s.index("    public Page<ExerciseResponse> getAdminExercisePage"):s.index("    private static <T extends Enum<T>> T parseEnum")]
print("OLD BLOCK", len(old))
new = (
    "    public Page<ExerciseResponse> getAdminExercisePage(Long lessonId, String type, String difficulty, String search, Pageable pageable) {\n"
    "        ExerciseType exerciseType = parseEnum(type, ExerciseType.class);\n"
    "        ExerciseDifficulty exerciseDifficulty = parseEnum(difficulty, ExerciseDifficulty.class);\n"
    "        Page<Exercise> page = exerciseRepository.findAdminPage(\n"
    "                lessonId,\n"
    "                exerciseType,\n"
    "                exerciseDifficulty,\n"
    "                search != null && !search.isBlank() ? search.trim() : null,\n"
    "                pageable);\n"
    "        // audit-v8 perf: read the row labels straight from the lesson table as id+title.\n"
    "        // Hydrating the joined Lesson entity pulled content/content_original (NVARCHAR MAX)\n"
    "        // for every row on the page, which was the single most read-heavy statement the app\n"
    "        // issues (95k logical reads per page). One extra batched query replaces that.\n"
    "        java.util.Set<Long> lessonIds = page.getContent().stream()\n"
    "                .map(e -> e.getLesson() == null ? null : e.getLesson().getId())\n"
    "                .filter(Objects::nonNull)\n"
    "                .collect(java.util.stream.Collectors.toSet());\n"
    "        Map<Long, String> titles = lessonIds.isEmpty() ? Map.of()\n"
    "                : lessonRepository.findTitlesById(lessonIds).stream()\n"
    "                        .collect(Collectors.toMap(LessonTitle::getLessonId, t -> t.getTitle() == null ? \"\" : t.getTitle(), (a, b) -> a));\n"
    "        return page.map(e -> toAdminRow(e, titles));\n"
    "    }\n\n"
    "    private ExerciseResponse toAdminRow(Exercise ex, Map<Long, String> titles) {\n"
    "        ExerciseResponse base = toResponse(ex, true);\n"
    "        if (!titles.isEmpty() && ex.getLesson() != null) {\n"
    "            base.setLessonTitle(titles.getOrDefault(ex.getLesson().getId(), base.getLessonTitle()));\n"
    "        }\n"
    "        return base;\n"
    "    }\n\n"
)
s = s.replace(old, new, 1)
if "import com.datn.engflow.model.dto.projection.LessonTitle;" not in s:
    s = s.replace("import com.datn.engflow.service.LessonContentService.LessonContentInfo;",
                  "import com.datn.engflow.model.dto.projection.LessonTitle;\nimport com.datn.engflow.service.LessonContentService.LessonContentInfo;", 1)
open(sp, "w", encoding="utf-8", newline="").write(s)
print("service patched")
