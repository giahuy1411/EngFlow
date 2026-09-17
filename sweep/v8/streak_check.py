import io, sys, json, urllib.request, urllib.error, time
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
B = "http://localhost:8080"
def call(m, p, tok=None, body=None):
    d = json.dumps(body).encode() if body is not None else None
    h = {}
    if tok: h["Authorization"] = "Bearer " + tok
    if d: h["Content-Type"] = "application/json"
    r = urllib.request.Request(B + p, data=d, method=m, headers=h)
    try:
        x = urllib.request.urlopen(r, timeout=60); return x.status, json.loads(x.read() or b"null")
    except urllib.error.HTTPError as e:
        try: return e.code, json.loads(e.read() or b"null")
        except Exception: return e.code, None

stamp = int(time.time()) % 100000
em = "zzsk%s@example.com" % stamp
c, j = call("POST", "/api/auth/register", None, {"username": "zzsk%d" % stamp, "email": em, "password": "Abcdef123!", "fullName": "ZZ Streak"})
print("register", c, (j or {}).get("id"))
tok = (j.get("data") or {}).get("token") or j.get("token")
c1, cur = call("GET", "/api/streak/current", tok)
print("streak before any study ->", c1, cur)
c2, hist = call("GET", "/api/streak/history?days=7", tok)
print("history before ->", c2, hist)

# a learner logs in again (records the day) then completes an exercise
c3, _ = call("POST", "/api/auth/login", None, {"email": em, "password": "Abcdef123!"})
j3 = c3 if isinstance(c3, int) else 0
c4, cur2 = call("GET", "/api/streak/current", tok)
print("streak after login ->", c4, cur2)
c5, hist2 = call("GET", "/api/streak/history?days=7", tok)
print("history after ->", c5, hist2)
c6, prog = call("GET", "/api/users/progress", tok)
print("progress ->", c6, prog)
c7, dash = call("GET", "/api/dashboard/stats", tok)
print("dashboard ->", c7, json.dumps(dash)[:200])
print("USER_ID", (j or {}).get("id"))
