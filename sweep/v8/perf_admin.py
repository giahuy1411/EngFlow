import io, sys, json, time, urllib.request
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
BASE = "http://localhost:8080"

def login(email, pw):
    data = json.dumps({"email": email, "password": pw}).encode()
    req = urllib.request.Request(BASE + "/api/auth/login", data=data, headers={"Content-Type": "application/json"})
    j = json.load(urllib.request.urlopen(req))
    d = j.get("data") or {}
    return d.get("token") or j.get("token")

def wait_up():
    for _ in range(60):
        try:
            urllib.request.urlopen(BASE + "/api/leaderboard?page=0&size=1", timeout=3); return True
        except Exception: time.sleep(3)
    return False

tok = login("admin@gmail.com", "123456")
print("up:", wait_up())

def call(path):
    req = urllib.request.Request(BASE + path, headers={"Authorization": "Bearer " + tok})
    t0 = time.perf_counter()
    try:
        r = urllib.request.urlopen(req, timeout=120)
        body = r.read()
        return r.status, (time.perf_counter() - t0) * 1000, len(body), body
    except urllib.error.HTTPError as e:
        return e.code, (time.perf_counter() - t0) * 1000, 0, e.read()

# warm the pool, then measure: no filter, lesson filter, keyword filter
for label, path in [
    ("page no filter", "/api/admin/exercises?page=0&size=20"),
    ("page lessonId", "/api/admin/exercises?lessonId=445&page=0&size=20"),
    ("page type", "/api/admin/exercises?type=MULTIPLE_CHOICE&page=0&size=20"),
    ("page keyword", "/api/admin/exercises?search=the&page=0&size=20"),
    ("page 2", "/api/admin/exercises?page=2&size=20"),
    ("page 200", "/api/admin/exercises?page=200&size=20"),
]:
    call(path)
    codes = []
    ms = []
    for _ in range(3):
        c, t, n, b = call(path)
        codes.append(c); ms.append(round(t))
    sample = json.loads(b) if codes[-1] == 200 else {}
    rows = sample.get("content", [])
    titled = sum(1 for r in rows if r.get("lessonTitle"))
    print("%-18s codes=%s ms=%s rows=%d withTitle=%d totalElements=%s"
          % (label, set(codes), ms, len(rows), titled, sample.get("totalElements")))
