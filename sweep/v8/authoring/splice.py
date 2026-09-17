import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="ascii", errors="backslashreplace")
p = "src/main/java/com/datn/engflow/service/AiExerciseService.java"
s = open(p, encoding="utf-8").read()
new = open("genall.txt", encoding="utf-8").read()

start = s.index("    public List<Exercise> generateAll(Lesson lesson, int count) {")
after = s.index("        return all;", start) + len("        return all;")
end = s.index(chr(10) + "    }", after) + len(chr(10) + "    }")
old = s[start:end]
print("OLD method:")
print(old.encode("ascii", "replace").decode())
s2 = s[:start] + new.rstrip() + s[end:]
open(p, "w", encoding="utf-8", newline="").write(s2)
print("spliced; delta chars", len(s2) - len(s))
