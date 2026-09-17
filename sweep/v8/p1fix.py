import io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
p = "sweep/v8/p1.js"
s = open(p, encoding="utf-8").read()
reps = [
 ('await probe("pub vocabulary list", "GET", "/api/vocabulary?page=0&size=5", "none", 200);',
  '// GET /api/vocabulary has no permitAll rule -> anyRequest().authenticated() = 401 by design;\n'
  '  // the UI list screen uses /api/vocabulary/search (permitAll). Guarded here so a future\n'
  '  // change to either is caught.\n'
  '  await probe("vocab list requires auth", "GET", "/api/vocabulary?page=0&size=5", "none", 401);\n'
  '  await probe("vocab list as user", "GET", "/api/vocabulary?page=0&size=5", "user", 200);'),
 ('await probe("media traversal attempt", "GET", "/api/v1/media/../../etc/passwd", "none", [400, 403, 404]);',
  '// %2e%2e encodings are decoded then rejected by the container filter (400), or fall\n'
  '  // through to anyRequest().authenticated() (401). Neither serves the file.\n'
  '  await probe("media traversal attempt", "GET", "/api/v1/media/%2e%2e%2f%2e%2e%2fetc/passwd", "none", [400, 401, 403]);'),
]
for a, b in reps:
    assert s.count(a) == 1, "anchor count " + str(s.count(a)) + " for " + a[:40]
    s = s.replace(a, b)
open(p, "w", encoding="utf-8", newline="").write(s)
print("p1 expectations corrected")
