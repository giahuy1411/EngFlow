import sys
p = "src/test/java/com/datn/engflow/controller/video/AuditV8VideoLessonUpdateTranscriptTest.java"
with open(p, "r", encoding="utf-8", newline="") as f:
    raw = f.read()
t = raw.replace("\r\n", "\n")

reps = [
("""        VideoLesson after = videoLessonRepository.findById(id).orElseThrow();
        assertThat(after.getTitle()).isEqualTo("ZZ keep transcript v2");
        assertThat(after.getTranscriptJson()).isEqualTo(TRANSCRIPT);
        assertThat(after.getCategory()).isEqualTo("KEEP");""",
"""        assertKeepsSeedTranscript(id, "ZZ keep transcript v2");
        assertThat(videoLessonRepository.findById(id).orElseThrow().getCategory()).isEqualTo("KEEP");"""),
("""        assertThat(videoLessonRepository.findById(id).orElseThrow().getTranscriptJson()).isEqualTo(TRANSCRIPT);
    }

    @Test
    void update_withEmptyTranscriptArray_keepsExistingTranscript""",
"""        assertKeepsSeedTranscript(id, "ZZ keep transcript v3");
    }

    @Test
    void update_withEmptyTranscriptArray_keepsExistingTranscript"""),
("""        assertThat(videoLessonRepository.findById(id).orElseThrow().getTranscriptJson()).isEqualTo(TRANSCRIPT);
    }

    @Test
    void create_withoutTranscript_returns400NotCreated""",
"""        assertKeepsSeedTranscript(id, "ZZ keep transcript v4");
    }

    @Test
    void create_withoutTranscript_returns400NotCreated"""),
]
for old, new in reps:
    if old not in t:
        print("NOT FOUND ::", old[:70].replace("\n", "\\n")); sys.exit(1)
    t = t.replace(old, new, 1)

assert "assertThat(after.getTranscriptJson())" not in t
assert t.count("assertKeepsSeedTranscript") == 4
with open(p, "w", encoding="utf-8", newline="") as f:
    f.write(t.replace("\n", "\r\n"))
print("patched", p)
