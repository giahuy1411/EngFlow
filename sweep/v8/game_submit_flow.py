import io, sys, json, urllib.request
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
BASE = "http://localhost:8080"

def call(path, tok=None, method="GET", body=None):
    h = {}
    data = None
    if tok: h["Authorization"] = "Bearer " + tok
    if body is not None:
        h["Content-Type"] = "application/json"; data = json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        r = urllib.request.urlopen(req, timeout=30)
        return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try: return e.code, json.loads(raw)
        except Exception: return e.code, raw[:200]

tok = call("/api/auth/login", method="POST",
           body={"email": "user@gmail.com", "password": "123456"})[1]
tok = (tok.get("data") or {}).get("token") or tok.get("token")

st, quiz = call("/api/games/quiz/10006", tok)
print("quiz:", st, "keys:", list(quiz.keys()) if isinstance(quiz, dict) else quiz)
sid = quiz.get("sessionId")
print("sessionId:", sid)
data = quiz.get("data")
print("data type:", type(data).__name__, "len:", len(data) if hasattr(data, "__len__") else "-")
if isinstance(data, list) and data:
    print("first item keys:", list(data[0].keys()))

st2, res = call("/api/games/submit", tok, "POST",
                {"sessionId": sid, "answers": None, "correctAnswers": 5})
print("submit count-only:", st2, res)
