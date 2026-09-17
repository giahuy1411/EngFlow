import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
p = "src/main/java/com/datn/engflow/service/AiExerciseService.java"
s = open(p, encoding="utf-8").read()

old = s[s.index("    public List<Exercise> generateAll(Lesson lesson, int count) {"):s.index("    // ?????? Review loop ??????")]
new = '''    public List<Exercise> generateAll(Lesson lesson, int count) {
        // audit-v8: `count` is a ceiling, not a suggestion. The old split handed
        // count/5 to every type and gave LISTENING the (possibly negative) remainder,
        // and each type could still overshoot -- measured count=3 persisted 30 rows.
        // Budget the requested number out one-at-a-time over the five types, then
        // trim any overshoot round-robin so a small count still yields a mix.
        int[] budget = new int[5];
        for (int i = 0; i < Math.max(0, count); i++) {
            budget[i % 5]++;
        }
        List<List<Exercise>> buckets = new ArrayList<>();
        buckets.add(budget[0] > 0 ? generateMultipleChoice(lesson, budget[0]) : new ArrayList<>());
        buckets.add(budget[1] > 0 ? generateFillBlank(lesson, budget[1]) : new ArrayList<>());
        buckets.add(budget[2] > 0 ? generateMatching(lesson, budget[2]) : new ArrayList<>());
        buckets.add(budget[3] > 0 ? generateTranslation(lesson, budget[3]) : new ArrayList<>());
        buckets.add(budget[4] > 0 ? generateListening(lesson, budget[4]) : new ArrayList<>());

        List<Exercise> all = new ArrayList<>();
        for (List<Exercise> bucket : buckets) {
            all.addAll(bucket);
        }
        if (all.size() > count) {
            List<Exercise> trimmed = new ArrayList<>(count);
            int[] cursor = new int[buckets.size()];
            while (trimmed.size() < count) {
                boolean took = false;
                for (int b = 0; b < buckets.size() && trimmed.size() < count; b++) {
                    List<Exercise> bucket = buckets.get(b);
                    if (cursor[b] < bucket.size()) {
                        trimmed.add(bucket.get(cursor[b]++));
                        took = true;
                    }
                }
                if (!took) {
                    break;
                }
            }
            log.info("generateAll trimmed {} accepted exercises down to the requested {} for lesson {}",
                    all.size(), trimmed.size(), lesson.getId());
            all = trimmed;
        }
        for (int i = 0; i < all.size(); i++) all.get(i).setOrderIndex(i);
        return all;
    }

'''
assert s.count(old) == 1
s = s.replace(old, new)
open(p, "w", encoding="utf-8", newline="").write(s)
print("rewrote generateAll")
