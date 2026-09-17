import io, sys, json, urllib.request
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

def login(email, pw):
    data = json.dumps({"email": email, "password": pw}).encode()
    req = urllib.request.Request("http://localhost:8080/api/auth/login", data=data,
                                 headers={"Content-Type": "application/json"})
    j = json.load(urllib.request.urlopen(req))
    d = j.get("data") or {}
    return d.get("token") or j.get("token")

tok = login("admin@gmail.com", "123456")
body = {"useAiReview": False, "exercises": [
    {"question": "Pick the comparative of good.", "options": json.dumps(["better", "gooder", "best", "best"]),
     "correctAnswer": "better", "exerciseType": "MULTIPLE_CHOICE", "explanation": "x"},
    {"question": "Pick the superlative of big.", "options": json.dumps(["biggest", "bigger", "big", "the big"]),
     "correctAnswer": "biggest", "exerciseType": "MULTIPLE_CHOICE", "explanation": "y"},
]}
req = urllib.request.Request("http://localhost:8080/api/admin/exercises/ai/validate",
                             data=json.dumps(body).encode(), method="POST",
                             headers={"Content-Type": "application/json", "Authorization": "Bearer " + tok})
r = json.load(urllib.request.urlopen(req))
print("valid=%s invalid=%s of %s" % (r["valid"], r["invalid"], r["total"]))
for s in r["schemaResults"]:
    print("   ", ("OK  " if s["valid"] else "REJ "), (s.get("question") or "")[:36], "|", s.get("error") or "")
