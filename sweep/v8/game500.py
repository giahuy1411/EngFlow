import io, sys, json, urllib.request, urllib.error
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BASE = "http://localhost:8080"


def login(email, pw):
    data = json.dumps({"email": email, "password": pw}).encode()
    req = urllib.request.Request(BASE + "/api/auth/login", data=data,
                                 headers={"Content-Type": "application/json"})
    j = json.load(urllib.request.urlopen(req))
    d = j.get("data") or {}
    return d.get("token") or j.get("token")


tok = login("user@gmail.com", "123456")
sid = json.load(urllib.request.urlopen(
    urllib.request.Request(BASE + "/api/games/quiz/10006",
                           headers={"Authorization": "Bearer " + tok})))["sessionId"]
print("session ok:", bool(sid))


def post(body):
    req = urllib.request.Request(BASE + "/api/games/submit",
                                data=json.dumps(body).encode(), method="POST",
                                headers={"Content-Type": "application/json",
                                         "Authorization": "Bearer " + tok})
    try:
        r = urllib.request.urlopen(req, timeout=30)
        return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


CASES = [
    ("correctAnswers object", {"sessionId": sid, "correctAnswers": {"a": 1}}),
    ("correctAnswers string", {"sessionId": sid, "correctAnswers": "9"}),
    ("sessionId object", {"sessionId": {"x": 1}, "correctAnswers": 1}),
    ("sessionId number", {"sessionId": 12345, "correctAnswers": 1}),
    ("answers object", {"sessionId": sid, "answers": {"a": 1}, "correctAnswers": 1}),
    ("answers list of strings", {"sessionId": sid, "answers": ["x"], "correctAnswers": 1}),
    ("answers list of numbers", {"sessionId": sid, "answers": [1, 2], "correctAnswers": 1}),
    ("legit client count", {"sessionId": sid, "correctAnswers": 1}),
    ("legit answers list", {"sessionId": sid, "answers": [], "correctAnswers": 0}),
]
for name, body in CASES:
    code, out = post(body)
    print("  %-26s -> %s %s%s" % (name, code, out[:80].replace("\n", " "),
                                  "   <== 500" if code == 500 else ""))
