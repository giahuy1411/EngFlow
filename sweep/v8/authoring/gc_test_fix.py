import io, sys, glob
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
p = glob.glob("src/test/java/**/GameControllerSubmitTypeTest.java", recursive=True)[0]
s = open(p, encoding="utf-8").read()

s = s.replace("UserPrincipal.create(findStudent())", "new UserPrincipal(findStudent())")

# submit consumes the Redis session key -> each case needs its own session
s = s.replace(
"""    @Autowired private com.datn.engflow.repository.UserRepository userRepository;""",
"""    @Autowired private com.datn.engflow.repository.UserRepository userRepository;
    @Autowired private com.datn.engflow.service.GameService gameService;

    /** A session that still exists: submit deletes the key, so one per case. */
    private String freshSession() {
        Map<String, Object> quiz = gameService.generateQuiz(10006L, findStudent().getId());
        return String.valueOf(quiz.get("sessionId"));
    }""")
s = s.replace('mvc.perform(post("/api/games/submit").with(principal()).contentType(MediaType.APPLICATION_JSON).content(\'{bad json\')', 'PLACEHOLDER')
open(p, "w", encoding="utf-8", newline="").write(s)
print("fixed principal + session helper")
print([l for l in s.splitlines() if "UserPrincipal" in l or "freshSession" in l])
