import io, sys, glob
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
p = glob.glob("src/main/java/**/AiExerciseService.java", recursive=True)[0]
s = open(p, encoding="utf-8", errors="replace").read()
i = s.index("private List<Exercise> generateWithReviewLoop")
seg = s[i:i+4200]
for k, ln in enumerate(seg.split(chr(10)), 1):
    if ("accepted.size" in ln) or ("break" in ln) or ("return accepted" in ln) or ("attempt" in ln):
        print(k, ln.strip())
print("=== count usage in buildPrompt ===")
j = s.index("String buildPrompt")
print(s[j:j+700].encode("ascii", "replace").decode())
