import io
p = "AGENTS.md"
t = io.open(p, encoding="utf-8", newline="").read()
crlf = t.count("\r\n") == t.count("\n")
t = t.replace("\r\n", "\n")
a = "baseline xanh: **354 tests**"
b = "baseline xanh: **369 tests**"
if a not in t: print("A NOT FOUND"); raise SystemExit(1)
t = t.replace(a, b, 1)
c = "3 rate-limit bucket routing, 3 AI parse/guard)."
d = ("3 rate-limit bucket routing, 3 AI parse/guard; v\u00f2ng 3 l\u1ea7n 2: 15 test F88/F89 \u2014 5 `AuditV8VideoLessonUpdateTranscriptTest`, "
     "7 `AuditV8DraftLessonVisibilityTest`, 3 `AuditV8VideoLessonDraftVisibilityTest`).")
if c not in t: print("C NOT FOUND"); raise SystemExit(1)
t = t.replace(c, d, 1)
e = "baseline: **82 tests / 17 files** (audit-v8 +3 `src/utils/sanitize-a11y.test.js`)."
f = "baseline: **85 tests / 18 files** (audit-v8 +3 `src/utils/sanitize-a11y.test.js`, +3 `src/views/admin/AdminVideoLessons.test.js`)."
if e not in t: print("E NOT FOUND"); raise SystemExit(1)
t = t.replace(e, f, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", "\r\n") if crlf else t)
print("ok")
