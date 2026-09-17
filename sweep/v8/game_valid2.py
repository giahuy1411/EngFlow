import io, sys, json, urllib.request, urllib.error
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
BASE = "http://localhost:8080"

def login(email, pw):
    data = json.dumps({"email": email, "password": pw}).encode()
    req = urllib.request.Request(BASE + "/api/auth/login", data=data, headers={"Content-Type": "application/json"})
    j = json.load(urllib.request.urlopen(req))
    d = j.get("data") or {}
    return d.get("token") or j.get("token")

tok = login("user@gmail.com", "123456")

def call(method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Authorization": "Bearer " + tok}
    if data:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
    try:
        r = urllib.request.urlopen(req, timeout=30)
        return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()

c, b = call("GET", "/api/games/quiz/10006")
print("start ->", c, b[:120])
g = json.loads(b)
sid, items = g["sessionId"], g["data"]
print("session", sid, "items", len(items))

# exactly what QuizGame.vue sends: {vocabId, answer} plus a client count
ans = [{"vocabId": it["vocabId"], "answer": it.get("options", [None])[0]} for it in items]
c, b = call("POST", "/api/games/submit", {"sessionId": sid, "answers": ans, "correctAnswers": 0})
print("ui-shape submit ->", c, b[:220])
