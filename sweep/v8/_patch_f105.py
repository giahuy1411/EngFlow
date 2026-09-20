p = "src/main/java/com/datn/engflow/controller/LessonExerciseController.java"
t = open(p, encoding="utf-8").read()

old_grade = """    @PostMapping("/grade")
    public ResponseEntity<GradeResponse> gradeExercises(
            @PathVariable Long lessonId,
            @RequestBody GradeRequest request) {
        GradeResponse response = exerciseService.gradeExercises(lessonId, request);"""
new_grade = """    @PostMapping("/grade")
    public ResponseEntity<GradeResponse> gradeExercises(
            @PathVariable Long lessonId,
            @RequestBody GradeRequest request,
            Authentication authentication) {
        // audit-v9 F105: draft lesson must not leak correctAnswer via grade/submit.
        lessonService.assertLessonVisible(lessonId, isAdmin(authentication));
        GradeResponse response = exerciseService.gradeExercises(lessonId, request);"""

old_sub = """            Authentication authentication) {
        GradeResponse response = exerciseService.submitExercises(lessonId, request, authentication.getName());"""
new_sub = """            Authentication authentication) {
        // audit-v9 F105: same guard as grade.
        lessonService.assertLessonVisible(lessonId, isAdmin(authentication));
        GradeResponse response = exerciseService.submitExercises(lessonId, request, authentication.getName());"""

assert old_grade in t, "grade anchor missing"
t = t.replace(old_grade, new_grade)
assert old_sub in t, "submit anchor missing"
t = t.replace(old_sub, new_sub)
open(p, "w", encoding="utf-8", newline="\r\n").write(t)
print("PATCHED_OK")
