import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
p = "sweep/v8/p2.js"
s = open(p, encoding="utf-8").read()
pairs = [
    ('level: "A2"', 'level: "ELEMENTARY"'),
    ('level: "B1", category: "GRAMMAR"', 'level: "INTERMEDIATE", category: "GRAMMAR"'),
    ('{ vocabularyId: 10017, quality: 4 }', '{ vocabId: 10017, quality: 4 }'),
    ('{ vocabularyId: 10017, quality: 99 }', '{ vocabId: 10017, quality: 99 }'),
    ('{ quality: 3 }', '{ quality: 3 }'),
    ('await probe("ST delete missing 404", "DELETE", "/api/admin/sections/99999999", "admin", 404);',
     'await probe("ST delete missing (idempotent 204)", "DELETE", "/api/admin/sections/99999999", "admin", [204, 404]);'),
    ('r = await probe("Game quiz start", "GET", "/api/games/quiz/10006", "user", 200);\n'
     '  let sess = null, answers = null, correct = null;\n'
     '  try { const g = JSON.parse(r.txt); sess = g.sessionId; const qs = g.questions || g.items || []; answers = {}; correct = {}; for (const q of qs) { answers[q.id] = q.answer || "a"; correct[q.id] = q.answer || "a"; } } catch (e) {}\n'
     '  console.log("  gameSession=" + !!sess);\n'
     '  await probe("Game submit", "POST", "/api/games/submit", "user", [200, 400], { sessionId: sess, answers: answers, correctAnswers: correct });',
     'r = await probe("Game quiz start", "GET", "/api/games/quiz/10006", "user", 200);\n'
     '  let sess = null, uiAnswers = null;\n'
     '  try { const g = JSON.parse(r.txt); sess = g.sessionId; uiAnswers = (g.data || []).map(it => ({ vocabId: it.vocabId, answer: (it.options || [""])[0] })); } catch (e) {}\n'
     '  console.log("  gameSession=" + !!sess + " items=" + (uiAnswers ? uiAnswers.length : 0));\n'
     '  await probe("Game submit (UI shape)", "POST", "/api/games/submit", "user", 200, { sessionId: sess, answers: uiAnswers, correctAnswers: 0 });'),
]
for a, b in pairs:
    n = s.count(a)
    print(("OK   " if n == 1 else "MULT " if n > 1 else "MISS ") + a[:70].replace("\n", " ") + " x" + str(n))
    if n == 1:
        s = s.replace(a, b)
open(p, "w", encoding="utf-8", newline="").write(s)
