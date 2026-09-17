import io, sys
p = "frontend/src/views/admin/AdminVideoLessons.vue"
raw = io.open(p, encoding="utf-8", newline="").read()
eol = "\r\n" if raw.count("\r\n") == raw.count("\n") else "\n"
t = raw.replace("\r\n", "\n")
old = """  // audit-v6 F21: keep transcript empty in the box — an empty box now means
  // "gi\u1eef ph\u1ee5 \u0111\u1ec1 c\u0169" (backend keeps current transcript on update).
  form.value = lesson
    ? { title: lesson.title, description: lesson.description || '', youtubeUrl: lesson.youtubeUrl || '', level: lesson.level || 'ELEMENTARY', transcriptText: '' }
    : defaultForm()"""
new = """  // audit-v6 F21: keep transcript empty in the box — an empty box now means
  // "gi\u1eef ph\u1ee5 \u0111\u1ec1 c\u0169" (backend keeps current transcript on update).
  //
  // audit-v8 F88a: list admin tr\u1ea3 VideoLessonSummary (c\u00f3 youtubeVideoId, KH\u00d4NG c\u00f3
  // youtubeUrl), n\u00ean \u0111\u1ecdc lesson.youtubeUrl ra undefined -> \u00f4 "Link YouTube" (required)
  // tr\u1ed1ng -> tr\u00ecnh duy\u1ec7t ch\u1eb7n submit, kh\u00f4ng c\u00f3 request n\u00e0o v\u00e0 kh\u00f4ng c\u00f3 toast: n\u00fat L\u01b0u im l\u1eb7ng.
  // D\u1ef1ng l\u1ea1i URL t\u1eeb video id \u0111\u1ec3 form s\u1eeda c\u00f3 \u0111\u1ee7 d\u1eef li\u1ec7u.
  form.value = lesson
    ? {
        title: lesson.title,
        description: lesson.description || '',
        youtubeUrl: lesson.youtubeUrl || (lesson.youtubeVideoId ? `https://www.youtube.com/watch?v=${lesson.youtubeVideoId}` : ''),
        level: lesson.level || 'ELEMENTARY',
        transcriptText: ''
      }
    : defaultForm()"""
if old not in t:
    print("NOT FOUND"); sys.exit(1)
t = t.replace(old, new, 1)
io.open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", eol))
print("patched", p, "eol=", repr(eol))
