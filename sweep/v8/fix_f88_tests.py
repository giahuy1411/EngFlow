import sys
p = "src/test/java/com/datn/engflow/controller/video/AuditV8VideoLessonUpdateTranscriptTest.java"
with open(p, "r", encoding="utf-8", newline="") as f:
    t = f.read()

t = t.replace("""import com.datn.engflow.model.entity.User;""",
"""import com.datn.engflow.model.dto.video.VideoDtos.TranscriptLine;
import com.datn.engflow.model.entity.User;""", 1)
t = t.replace("""import com.datn.engflow.security.UserPrincipal;""",
"""import com.datn.engflow.security.UserPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;""", 1)
t = t.replace("""import org.springframework.web.context.WebApplicationContext;""",
"""import org.springframework.web.context.WebApplicationContext;

import java.util.List;""", 1)

helper = """    private Long seedLesson() {"""
new_helper = """    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Doc transcript tu cot JSON ra record de so sanh theo ngu nghia (bo qua format/thu tu field). */
    private static List<TranscriptLine> transcriptOf(VideoLesson lesson) throws Exception {
        return MAPPER.readValue(lesson.getTranscriptJson(), new TypeReference<List<TranscriptLine>>() { });
    }

    private void assertKeepsSeedTranscript(Long id, String expectedTitle) throws Exception {
        VideoLesson after = videoLessonRepository.findById(id).orElseThrow();
        assertThat(after.getTitle()).isEqualTo(expectedTitle);
        List<TranscriptLine> lines = transcriptOf(after);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).textEn()).isEqualTo("Hello there.");
        assertThat(lines.get(1).textEn()).isEqualTo("One coffee please.");
    }

    private Long seedLesson() {"""
t = t.replace(helper, new_helper, 1)

old1 = """        VideoLesson after = videoLessonRepository.findById(id).orElseThrow();
        assertThat(after.getTitle()).isEqualTo("ZZ keep transcript v2");
        assertThat(after.getTranscriptJson()).isEqualTo(TRANSCRIPT);
        assertThat(after.getCategory()).isEqualTo("KEEP");"""
new1 = """        assertKeepsSeedTranscript(id, "ZZ keep transcript v2");
        assertThat(videoLessonRepository.findById(id).orElseThrow().getCategory()).isEqualTo("KEEP");"""
t = t.replace(old1, new1, 1)

old2 = """        assertThat(videoLessonRepository.findById(id).orElseThrow().getTranscriptJson()).isEqualTo(TRANSCRIPT);
    }

    @Test
    void update_withEmptyTranscriptArray_keepsExistingTranscript"""
new2 = """        assertKeepsSeedTranscript(id, "ZZ keep transcript v3");
    }

    @Test
    void update_withEmptyTranscriptArray_keepsExistingTranscript"""
t = t.replace(old2, new2, 1)

old3 = """        assertThat(videoLessonRepository.findById(id).orElseThrow().getTranscriptJson()).isEqualTo(TRANSCRIPT);
    }

    @Test
    void create_withoutTranscript_returns400NotCreated"""
new3 = """        assertKeepsSeedTranscript(id, "ZZ keep transcript v4");
    }

    @Test
    void create_withoutTranscript_returns400NotCreated"""
t = t.replace(old3, new3, 1)

with open(p, "w", encoding="utf-8", newline="") as f:
    f.write(t)
print("patched", p)
assert "assertThat(after.getTranscriptJson())" not in t
assert "assertKeepsSeedTranscript" in t
