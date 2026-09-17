import io, glob
files = [
  "src/test/java/com/datn/engflow/controller/video/AuditV8VideoLessonUpdateTranscriptTest.java",
  "src/test/java/com/datn/engflow/controller/AuditV8DraftLessonVisibilityTest.java",
  "src/test/java/com/datn/engflow/controller/video/AuditV8VideoLessonDraftVisibilityTest.java",
  "src/main/java/com/datn/engflow/model/dto/video/VideoDtos.java",
  "src/main/java/com/datn/engflow/service/VideoLessonService.java",
  "src/main/java/com/datn/engflow/service/LessonService.java",
  "frontend/src/views/admin/AdminVideoLessons.vue",
  "sweep/v8/ui/v4edit.js",
  "sweep/v8/p88live.js",
  "sweep/v8/patch_p4_f88.py",
  "sweep/v8/add_f88_tests.py",
  "sweep/v8/fix_f88_tests.py",
]
for p in files:
    try:
        t = io.open(p, encoding="utf-8", newline="").read()
    except FileNotFoundError:
        print("skip (missing)", p); continue
    n = t.replace("audit-v8 F88a", "audit-v8 F88").replace("audit-v8 F88b", "audit-v8 F89")
    if n != t:
        io.open(p, "w", encoding="utf-8", newline="").write(n)
        print("renamed labels in", p)
    else:
        print("no change", p)
