import io, sys, json, urllib.request
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
B = "http://localhost:8080"


def call(path, method="GET", tok=None, body=None):
    h = {}
    if tok: h["Authorization"] = "Bearer " + tok
    data = None
    if body is not None:
        data = json.dumps(body).encode(); h["Content-Type"] = "application/json"
    req = urllib.request.Request(B + path, data=data, headers=h, method=method)
    try:
        r = urllib.request.urlopen(req, timeout=60)
        return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")[:160]


tok = json.loads(call("/api/auth/login", "POST", body={"email": "user@gmail.com", "password": "123456"})[1])
tok = (json.loads(call("/api/auth/login", "POST", body={"email": "user@gmail.com", "password": "123456"})[1]).get("data") or {}).get("token")

st, g = call("/api/games/quiz/10006", tok=tok)
g = json.loads(g)
sid, items = g["sessionId"], g["data"]
print("session:", sid[:8], "questions:", len(items))

# what the UI actually sends: answers = [{vocabId, answer}], correctAnswers = client tally
vid = items[0]["vocabId"]
right = items[0]["options"].index(items[0]["word"]) if False else None
answers = [{"vocabId": it["vocabId"], "answer": it["options"][0]} for it in items]
st, r = call("/api/games/submit", "POST", tok, {"sessionId": sid, "answers": answers, "correctAnswers": len(items)})
print("server-validated submit ->", st, r[:200])

# second submit of same session must not double-award
st2, r2 = call("/api/games/submit", "POST", tok, {"sessionId": sid, "answers": answers, "correctAnswers": len(items)})
print("replay submit         ->", st2, r2[:120])
