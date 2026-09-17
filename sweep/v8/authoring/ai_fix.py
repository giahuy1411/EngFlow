import io, sys, glob
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

# ---- fix 1: generateAll must honour the requested count ----
p = glob.glob("src/main/java/**/AiExerciseService.java", recursive=True)[0]
s = open(p, encoding="utf-8").read()
old = """        for (int i = 0; i < all.size(); i++) all.get(i).setOrderIndex(i);
        return all;
    }"""
new = """        // audit-v8: `count` is a ceiling, not a suggestion. The review loop stops as
        // soon as it has AT LEAST count accepted items per type, so five types can
        // overshoot (measured: count=3 produced 30 saved rows). Trim, then renumber,
        // so the API returns and persists exactly what was asked for.
        if (all.size() > count) {
            all = new ArrayList<>(all.subList(0, count));
        }
        for (int i = 0; i < all.size(); i++) all.get(i).setOrderIndex(i);
        return all;
    }"""
assert s.count(old) == 1, "generateAll anchor=" + str(s.count(old))
s = s.replace(old, new)
open(p, "w", encoding="utf-8", newline="").write(s)
print("OK fix1 generateAll ceiling")

# ---- fix 2: ai-generate must reject a blank topic before spending an LLM call ----
q = glob.glob("src/main/java/**/SpeakingPromptController.java", recursive=True)[0]
t = open(q, encoding="utf-8").read()
o2 = """        String topic = body.getOrDefault("topic", "");
        String level = body.getOrDefault("level", "B1");"""
n2 = """        String topic = body.getOrDefault("topic", "");
        if (topic.isBlank()) {
            // audit-v8: ai-generate-full already guards this; without the same check an
            // empty topic still reached Ollama and came back as invented filler.
            return ResponseEntity.badRequest().body(Map.of("error", "Cần nhập chủ đề"));
        }
        String level = body.getOrDefault("level", "B1");"""
assert t.count(o2) == 1, "topic anchor=" + str(t.count(o2))
t = t.replace(o2, n2)
open(q, "w", encoding="utf-8", newline="").write(t)
print("OK fix2 blank topic guard")
