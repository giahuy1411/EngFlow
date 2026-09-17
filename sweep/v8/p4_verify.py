import io, sys, json, time, urllib.request, urllib.error
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
BASE = "http://localhost:8080"

def login(email, pw):
    data = json.dumps({"email": email, "password": pw}).encode()
    req = urllib.request.Request(BASE + "/api/auth/login", data=data, headers={"Content-Type": "application/json"})
    j = json.load(urllib.request.urlopen(req))
    d = j.get("data") or {}
    return d.get("token") or j.get("token")

def call(method, path, tok=None, body=None, timeout=300):
    data = json.dumps(body).encode() if body is not None else None
    h = {}
    if tok: h["Authorization"] = "Bearer " + tok
    if data: h["Content-Type"] = "application/json"
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=h)
    t0 = time.time()
    try:
        r = urllib.request.urlopen(req, timeout=timeout)
        return r.status, r.read().decode("utf-8", "replace"), time.time() - t0
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace"), time.time() - t0

admin = login("admin@gmail.com", "123456")
user = login("user@gmail.com", "123456")

print("== F81 upload allowlist (user path) ==")
for fn in ["x.html", "x.svg", "x.js", "ok.webm", "ok.png"]:
    code, out, _ = call("POST", "/api/lesson-submissions/upload-audio", user, {"f": fn}) if False else (None, None, None)
print("   (multipart covered by verify_fix.py — skipping here)")

print("== game submit type guards ==")
c, b, _ = call("GET", "/api/games/quiz/10006", user)
sid = json.loads(b)["sessionId"]
for name, payload in [
    ("correctAnswers object", {"sessionId": sid, "correctAnswers": {"a": 1}}),
    ("sessionId number", {"sessionId": 12345, "correctAnswers": 3}),
    ("answers object", {"sessionId": sid, "answers": {"a": 1}, "correctAnswers": 3}),
]:
    c, b, _ = call("POST", "/api/games/submit", user, payload)
    print("   %-22s -> %s %s" % (name, c, b[:80]))

print("== blank-topic ai-generate guard ==")
c, b, t = call("POST", "/api/v1/admin/speaking-prompts/ai-generate", admin, {"topic": "", "level": "B1"})
print("   blank topic ->", c, b[:80], "(%.1fs)" % t)
c, b, t = call("POST", "/api/v1/admin/speaking-prompts/ai-generate", admin, {"topic": "travel", "level": "A2"})
print("   real topic  ->", c, "(%.1fs) %s" % (t, b[:90]))

print("== generate-all count ceiling (count=5, 5 types) ==")
content = ("We form the comparative with -er for short adjectives: tall to taller. "
           "We use more for long adjectives: expensive to more expensive. "
           "The superlative adds -est: the tallest. Irregular: good better best. ") * 3
c, b, _ = call("POST", "/api/admin/lessons", admin, {"title": "ZZ v8p4 ceiling " + str(int(time.time())),
            "content": content, "level": "ELEMENTARY", "category": "GRAMMAR", "durationMinutes": 5,
            "isPublished": False, "orderIndex": 9997})
lid = json.loads(b)["id"]
c, b, t = call("POST", "/api/admin/exercises/ai/generate-all", admin, {"lessonId": lid, "count": 5}, timeout=600)
print("   generate-all ->", c, "%.0fs" % t)
try:
    j = json.loads(b)
    got = j.get("generated")
    types = {}
    for e in j.get("exercises", []):
        types[e.get("exerciseType")] = types.get(e.get("exerciseType"), 0) + 1
    print("   requested=5 generated=%s types=%s" % (got, json.dumps(types)))
    c2, b2, _ = call("GET", "/api/admin/exercises?lessonId=%s&page=0&size=50" % lid, admin)
    page = json.loads(b2)
    persisted = page.get("totalElements")
    print("   persisted rows in DB =", persisted)
    print("   VERDICT:", "PASS (<=5 and diverse)" if (got or 0) <= 5 and len(types) > 1 else "CHECK", "| db==gen:", persisted == got)
except Exception as e:
    print("   parse err", e, b[:300])
finally:
    call("DELETE", "/api/admin/lessons/%s" % lid, admin)
    c3, b3, _ = call("GET", "/api/admin/exercises?lessonId=%s&page=0&size=50" % lid, admin)
    print("   after delete: leftover exercises =", json.loads(b3).get("totalElements"))
