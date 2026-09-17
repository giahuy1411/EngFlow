import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")
p = "src/test/java/com/datn/engflow/service/AiExerciseServiceParsingTest.java"
s = open(p, encoding="utf-8").read()
test = """
    // ── audit-v8: MULTIPLE_CHOICE option quality ──

    @Test
    @DisplayName("validateSchema: rejects MULTIPLE_CHOICE with duplicate options")
    void rejectsDuplicateOptions() throws Exception {
        // measured model failure mode: ["most expensive","more expensive","best","best"]
        Exercise ex = Exercise.builder()
                .exerciseType(ExerciseType.MULTIPLE_CHOICE)
                .question("What is the superlative of expensive?")
                .options(new ObjectMapper().writeValueAsString(
                        List.of("most expensive", "more expensive", "best", "best")))
                .correctAnswer("best")
                .build();
        assertThat(service.validateSchema(ex)).contains("distinct");
    }

    @Test
    @DisplayName("validateSchema: duplicate check ignores case and surrounding space")
    void rejectsDuplicateOptionsIgnoringCase() throws Exception {
        Exercise ex = Exercise.builder()
                .exerciseType(ExerciseType.MULTIPLE_CHOICE)
                .question("Pick one:")
                .options(new ObjectMapper().writeValueAsString(List.of(" have ", "HAS", "has")))
                .correctAnswer("has")
                .build();
        assertThat(service.validateSchema(ex)).contains("distinct");
    }

    @Test
    @DisplayName("validateSchema: distinct options still accepted")
    void acceptsDistinctOptions() throws Exception {
        Exercise ex = Exercise.builder()
                .exerciseType(ExerciseType.MULTIPLE_CHOICE)
                .question("Pick one:")
                .options(new ObjectMapper().writeValueAsString(List.of("have", "has", "had", "having")))
                .correctAnswer("has")
                .build();
        assertThat(service.validateSchema(ex)).isNull();
    }
"""
i = s.rstrip().rfind("}")
s2 = s[:i] + test + s[i:]
open(p, "w", encoding="utf-8", newline="").write(s2)
print("inserted at", i, "file len", len(s2))
